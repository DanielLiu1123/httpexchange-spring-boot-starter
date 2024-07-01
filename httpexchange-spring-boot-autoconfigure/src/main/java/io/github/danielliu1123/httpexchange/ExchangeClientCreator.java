package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.REST_CLIENT;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.REST_TEMPLATE;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.WEB_CLIENT;
import static io.github.danielliu1123.httpexchange.Util.findMatchedConfig;
import static io.github.danielliu1123.httpexchange.Util.hasAnnotation;
import static io.github.danielliu1123.httpexchange.Util.isHttpExchangeInterface;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.danielliu1123.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class ExchangeClientCreator {
    private static final Logger log = LoggerFactory.getLogger(ExchangeClientCreator.class);

    private static final boolean WEBFLUX_PRESENT =
            ClassUtils.isPresent("org.springframework.web.reactive.function.client.WebClient", null);
    private static final boolean LOADBALANCER_PRESENT =
            ClassUtils.isPresent("org.springframework.cloud.client.loadbalancer.LoadBalancerClient", null);
    private static final boolean DEFERRING_LOADBALANCER_INTERCEPTOR_PRESENT = ClassUtils.isPresent(
            "org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor", null);

    private static final Field exchangeAdapterField;
    private static final Field customArgumentResolversField;
    private static final Field conversionServiceField;
    private static final Field embeddedValueResolverField;

    static {
        try {
            Class<HttpServiceProxyFactory.Builder> clz = HttpServiceProxyFactory.Builder.class;
            exchangeAdapterField = clz.getDeclaredField("exchangeAdapter");
            customArgumentResolversField = clz.getDeclaredField("customArgumentResolvers");
            conversionServiceField = clz.getDeclaredField("conversionService");
            embeddedValueResolverField = clz.getDeclaredField("embeddedValueResolver");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private final BeanFactory beanFactory;
    private final Environment environment;
    private final Class<?> clientType;
    private final boolean isUseHttpExchangeAnnotation;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ExchangeClientCreator(BeanFactory beanFactory, Class<?> clientType) {
        this.beanFactory = beanFactory;
        this.environment = beanFactory.getBean(Environment.class);

        Assert.isTrue(clientType.isInterface(), () -> clientType + " is not an interface");
        this.clientType = clientType;

        Assert.isTrue(isHttpExchangeInterface(clientType), () -> clientType + " is not a HttpExchange client");
        this.isUseHttpExchangeAnnotation = hasAnnotation(clientType, HttpExchange.class);
    }

    /**
     * Create a proxy {@link HttpExchange}/{@link RequestMapping} interface instance.
     *
     * @param <T> type of the {@link HttpExchange}/{@link RequestMapping} interface
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        HttpExchangeProperties httpExchangeProperties = beanFactory
                .getBeanProvider(HttpExchangeProperties.class)
                .getIfUnique(() -> Util.getProperties(environment));
        HttpExchangeProperties.Channel chan =
                findMatchedConfig(clientType, httpExchangeProperties).orElseGet(httpExchangeProperties::defaultClient);
        if (isUseHttpExchangeAnnotation) {
            HttpServiceProxyFactory factory = factoryBuilder(chan).build();
            T result = (T) factory.createClient(clientType);
            Cache.addClient(result);
            return result;
        }
        if (!httpExchangeProperties.isRequestMappingSupportEnabled()) {
            throw new IllegalStateException(
                    clientType
                            + " is using the @RequestMapping based annotation, please migrate to @HttpExchange, or set 'http-exchange.request-mapping-support-enabled=true' to enable support for processing @RequestMapping");
        }
        ShadedHttpServiceProxyFactory shadedFactory =
                shadedProxyFactory(factoryBuilder(chan)).build();
        T result = (T) shadedFactory.createClient(clientType);
        Cache.addClient(result);
        return result;
    }

    private HttpServiceProxyFactory.Builder factoryBuilder(HttpExchangeProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder builder = HttpServiceProxyFactory.builder();

        beanFactory
                .getBeanProvider(HttpServiceProxyFactoryCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(builder));

        setExchangeAdapter(builder, channelConfig);

        setEmbeddedValueResolver(builder);

        addCustomArgumentResolver(builder);

        return builder;
    }

    private void setExchangeAdapter(
            HttpServiceProxyFactory.Builder builder, HttpExchangeProperties.Channel channelConfig) {
        if (WEBFLUX_PRESENT && hasReactiveReturnTypeMethod(clientType)) {
            HttpExchangeProperties.ClientType type = channelConfig.getClientType();
            if (type != null && type != WEB_CLIENT) {
                log.warn(
                        "{} contains methods with reactive return types, should use the client-type '{}' instead of '{}'",
                        clientType.getSimpleName(),
                        WEB_CLIENT,
                        type);
            }
            builder.exchangeAdapter(WebClientAdapter.create(
                    getClient(new Cache.ClientId(channelConfig, WEB_CLIENT), () -> buildWebClient(channelConfig))));
            return;
        }

        switch (getClientType(channelConfig)) {
            case REST_CLIENT -> builder.exchangeAdapter(RestClientAdapter.create(
                    getClient(new Cache.ClientId(channelConfig, REST_CLIENT), () -> buildRestClient(channelConfig))));
            case REST_TEMPLATE -> builder.exchangeAdapter(RestTemplateAdapter.create(getClient(
                    new Cache.ClientId(channelConfig, REST_TEMPLATE), () -> buildRestTemplate(channelConfig))));
            case WEB_CLIENT -> {
                if (WEBFLUX_PRESENT) {
                    builder.exchangeAdapter(WebClientAdapter.create(getClient(
                            new Cache.ClientId(channelConfig, WEB_CLIENT), () -> buildWebClient(channelConfig))));
                } else {
                    log.warn(
                            "Since spring-webflux is not in the classpath, the client-type will fall back to '{}'",
                            REST_CLIENT);
                    builder.exchangeAdapter(RestClientAdapter.create(getClient(
                            new Cache.ClientId(channelConfig, REST_CLIENT), () -> buildRestClient(channelConfig))));
                }
            }
            default -> throw new IllegalStateException("Unsupported client-type: " + channelConfig.getClientType());
        }
    }

    private static <T> T getClient(Cache.ClientId clientId, Supplier<T> supplier) {
        return Boolean.TRUE.equals(clientId.channel().getHttpClientReuseEnabled())
                ? Cache.getHttpClient(clientId, supplier)
                : supplier.get();
    }

    private void addCustomArgumentResolver(HttpServiceProxyFactory.Builder builder) {
        List<HttpServiceArgumentResolver> existingResolvers = getFieldValue(builder, customArgumentResolversField);
        beanFactory
                .getBeanProvider(HttpServiceArgumentResolver.class)
                .orderedStream()
                .filter(resolver -> !existingResolvers.contains(resolver))
                .forEach(builder::customArgumentResolver);
    }

    private void setEmbeddedValueResolver(HttpServiceProxyFactory.Builder builder) {
        // String value resolver, need to support ${} placeholder
        StringValueResolver resolver = Optional.ofNullable(getFieldValue(builder, embeddedValueResolverField))
                .map(StringValueResolver.class::cast)
                .map(r -> UrlPlaceholderStringValueResolver.create(environment, r))
                .orElseGet(() -> UrlPlaceholderStringValueResolver.create(environment, null));
        builder.embeddedValueResolver(resolver);
    }

    private RestTemplate buildRestTemplate(HttpExchangeProperties.Channel channelConfig) {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplateBuilderConfigurer configurer =
                beanFactory.getBeanProvider(RestTemplateBuilderConfigurer.class).getIfUnique();
        if (configurer != null) {
            builder = configurer.configure(builder);
        }
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder = builder.rootUri(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            for (HttpExchangeProperties.Header header : channelConfig.getHeaders()) {
                builder = builder.defaultHeader(
                        header.getKey(), header.getValues().toArray(String[]::new));
            }
        }

        // Set default request factory
        builder = builder.requestFactory(() -> getRequestFactory(channelConfig));

        if (isLoadBalancerEnabled(channelConfig)) {
            Set<ClientHttpRequestInterceptor> lbInterceptors = new LinkedHashSet<>();
            if (DEFERRING_LOADBALANCER_INTERCEPTOR_PRESENT) {
                beanFactory
                        .getBeanProvider(DeferringLoadBalancerInterceptor.class)
                        .forEach(lbInterceptors::add);
            } else {
                beanFactory.getBeanProvider(ClientHttpRequestInterceptor.class).forEach(lbInterceptors::add);
            }
            builder = builder.additionalInterceptors(lbInterceptors);
        }

        // Default request factory will be replaced by user's RestTemplateCustomizer bean here
        RestTemplate restTemplate = builder.build();

        // Remove duplicates and reorder
        restTemplate.setInterceptors(
                restTemplate.getInterceptors().stream().distinct().toList());

        setTimeoutByConfig(restTemplate.getRequestFactory(), channelConfig);

        beanFactory
                .getBeanProvider(HttpClientCustomizer.RestTemplateCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(restTemplate, channelConfig));

        return restTemplate;
    }

    private WebClient buildWebClient(HttpExchangeProperties.Channel channelConfig) {
        WebClient.Builder builder = WebClient.builder();
        beanFactory
                .getBeanProvider(WebClientCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(builder));
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.baseUrl(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        if (channelConfig.getReadTimeout() != null) {
            builder.filter((request, next) ->
                    next.exchange(request).timeout(Duration.ofMillis(channelConfig.getReadTimeout())));
        }
        if (isLoadBalancerEnabled(channelConfig)) {
            builder.filters(filters -> {
                Set<ExchangeFilterFunction> allFilters = new LinkedHashSet<>(filters);

                beanFactory
                        .getBeanProvider(DeferringLoadBalancerExchangeFilterFunction.class)
                        .forEach(allFilters::add);

                filters.clear();
                filters.addAll(allFilters);
                AnnotationAwareOrderComparator.sort(filters);
            });
        }

        beanFactory
                .getBeanProvider(HttpClientCustomizer.WebClientCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(builder, channelConfig));

        return builder.build();
    }

    private RestClient buildRestClient(HttpExchangeProperties.Channel channelConfig) {
        // Do not use RestClient.Builder bean here, because we can't know requestFactory is configured by user or not
        RestClient.Builder builder = RestClient.builder();
        beanFactory
                .getBeanProvider(RestClientBuilderConfigurer.class)
                .ifUnique(configurer -> configurer.configure(builder));
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.baseUrl(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }

        ClientHttpRequestFactory requestFactory =
                unwrapRequestFactoryIfNecessary(getFieldValue(builder, "requestFactory"));
        if (requestFactory == null) {
            builder.requestFactory(getRequestFactory(channelConfig));
        } else {
            setTimeoutByConfig(requestFactory, channelConfig);
        }

        if (isLoadBalancerEnabled(channelConfig)) {
            builder.requestInterceptors(interceptors -> {
                Set<ClientHttpRequestInterceptor> lbInterceptors = new LinkedHashSet<>(interceptors);
                if (DEFERRING_LOADBALANCER_INTERCEPTOR_PRESENT) {
                    beanFactory
                            .getBeanProvider(DeferringLoadBalancerInterceptor.class)
                            .forEach(lbInterceptors::add);
                } else {
                    beanFactory
                            .getBeanProvider(ClientHttpRequestInterceptor.class)
                            .forEach(lbInterceptors::add);
                }

                interceptors.clear();
                interceptors.addAll(lbInterceptors);
                AnnotationAwareOrderComparator.sort(interceptors);
            });
        }

        beanFactory
                .getBeanProvider(HttpClientCustomizer.RestClientCustomizer.class)
                .orderedStream()
                .forEach(customizer -> customizer.customize(builder, channelConfig));

        return builder.build();
    }

    private ClientHttpRequestFactory getRequestFactory(HttpExchangeProperties.Channel channelConfig) {
        ClientHttpRequestFactorySettings settings = new ClientHttpRequestFactorySettings(
                Optional.ofNullable(channelConfig.getConnectTimeout())
                        .map(Duration::ofMillis)
                        .orElse(null),
                Optional.ofNullable(channelConfig.getReadTimeout())
                        .map(Duration::ofMillis)
                        .orElse(null),
                (SslBundle) null);
        return ClientHttpRequestFactories.get(JdkClientHttpRequestFactory.class, settings);
    }

    private boolean isLoadBalancerEnabled(HttpExchangeProperties.Channel channelConfig) {
        return LOADBALANCER_PRESENT
                && environment.getProperty("spring.cloud.loadbalancer.enabled", Boolean.class, true)
                && channelConfig.getLoadbalancerEnabled();
    }

    private static String getRealBaseUrl(HttpExchangeProperties.Channel channelConfig) {
        String baseUrl = channelConfig.getBaseUrl();
        return baseUrl.contains("://") ? baseUrl : "http://" + baseUrl;
    }

    static ShadedHttpServiceProxyFactory.Builder shadedProxyFactory(HttpServiceProxyFactory.Builder proxyFactory) {
        HttpExchangeAdapter exchangeAdapter = getFieldValue(proxyFactory, exchangeAdapterField);
        List<HttpServiceArgumentResolver> customArgumentResolvers =
                getFieldValue(proxyFactory, customArgumentResolversField);
        ConversionService conversionService = getFieldValue(proxyFactory, conversionServiceField);
        StringValueResolver embeddedValueResolver = getFieldValue(proxyFactory, embeddedValueResolverField);

        ShadedHttpServiceProxyFactory.Builder builder = ShadedHttpServiceProxyFactory.builder();
        Optional.ofNullable(exchangeAdapter).ifPresent(builder::exchangeAdapter);
        Optional.ofNullable(customArgumentResolvers).stream()
                .flatMap(Collection::stream)
                .forEach(builder::customArgumentResolver);
        Optional.ofNullable(conversionService).ifPresent(builder::conversionService);
        Optional.ofNullable(embeddedValueResolver).ifPresent(builder::embeddedValueResolver);
        return builder;
    }

    /**
     * visible for testing
     */
    static boolean hasReactiveReturnTypeMethod(Class<?> clz) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(clz))
                .filter(method -> AnnotationUtils.findAnnotation(method, HttpExchange.class) != null
                        || AnnotationUtils.findAnnotation(method, RequestMapping.class) != null)
                .map(Method::getReturnType)
                .anyMatch(returnType -> Publisher.class.isAssignableFrom(returnType)
                        || Flow.Publisher.class.isAssignableFrom(returnType));
    }

    private static HttpExchangeProperties.ClientType getClientType(HttpExchangeProperties.Channel channel) {
        return channel.getClientType() != null ? channel.getClientType() : REST_CLIENT;
    }

    /**
     * @see ClientHttpRequestFactories.Reflective#unwrapRequestFactoryIfNecessary(ClientHttpRequestFactory)
     */
    private static ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(ClientHttpRequestFactory requestFactory) {
        if (requestFactory instanceof AbstractClientHttpRequestFactoryWrapper wrapper) {
            var delegate = wrapper.getDelegate();
            while (delegate instanceof AbstractClientHttpRequestFactoryWrapper w) {
                delegate = w.getDelegate();
            }
            return delegate;
        }
        return requestFactory;
    }

    private static void setTimeoutByConfig(
            ClientHttpRequestFactory requestFactory, HttpExchangeProperties.Channel channelConfig) {
        ClientHttpRequestFactory realRequestFactory = unwrapRequestFactoryIfNecessary(requestFactory);
        if (realRequestFactory == null) {
            return;
        }
        Optional.ofNullable(channelConfig.getReadTimeout())
                .ifPresent(readTimeout -> setTimeout(realRequestFactory, "setReadTimeout", readTimeout));
        Optional.ofNullable(channelConfig.getConnectTimeout())
                .ifPresent(connectTimeout -> setTimeout(realRequestFactory, "setConnectTimeout", connectTimeout));
    }

    private static void setTimeout(ClientHttpRequestFactory requestFactory, String method, int timeout) {
        if (!trySetTimeout(requestFactory, method, int.class, timeout)
                && !trySetTimeout(requestFactory, method, Duration.class, Duration.ofMillis(timeout))
                && !trySetTimeout(requestFactory, method, long.class, (long) timeout)) {
            log.warn(
                    "ClientHttpRequestFactory implementation {} not provide a method '{}' to modify the timeout",
                    requestFactory.getClass().getName(),
                    method);
        }
    }

    private static boolean trySetTimeout(
            ClientHttpRequestFactory requestFactory, String method, Class<?> paramType, Object paramValue) {
        Method m = ReflectionUtils.findMethod(requestFactory.getClass(), method, paramType);
        if (m != null) {
            ReflectionUtils.makeAccessible(m);
            ReflectionUtils.invokeMethod(m, requestFactory, paramValue);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, Field field) {
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, obj);
    }

    private static <T> T getFieldValue(Object obj, String fieldName) {
        Field field = ReflectionUtils.findField(obj.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        return getFieldValue(obj, field);
    }
}
