package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.REST_CLIENT;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.WEB_CLIENT;
import static io.github.danielliu1123.httpexchange.Util.findMatchedConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.danielliu1123.httpexchange.factory.jdkclient.EnhancedJdkClientHttpRequestFactory;
import io.github.danielliu1123.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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

    private final ConfigurableBeanFactory beanFactory;
    private final Environment environment;
    private final Class<?> clientType;
    private final boolean isUseHttpExchangeAnnotation;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    ExchangeClientCreator(
            ConfigurableBeanFactory beanFactory, Class<?> clientType, boolean isUseHttpExchangeAnnotation) {
        this.beanFactory = beanFactory;
        this.environment = beanFactory.getBean(Environment.class);
        this.clientType = clientType;
        this.isUseHttpExchangeAnnotation = isUseHttpExchangeAnnotation;
    }

    /**
     * Create a proxy {@link HttpExchange}/{@link RequestMapping} interface instance.
     *
     * @param <T> type of the {@link HttpExchange}/{@link RequestMapping} interface
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        if (log.isTraceEnabled()) {
            log.trace("Creating http exchange client for {}", clientType.getSimpleName());
        }

        HttpExchangeProperties httpExchangeProperties = beanFactory
                .getBeanProvider(HttpExchangeProperties.class)
                .getIfUnique(() -> Util.getProperties(environment));
        HttpExchangeProperties.Channel chan =
                findMatchedConfig(clientType, httpExchangeProperties).orElseGet(httpExchangeProperties::defaultClient);
        if (isUseHttpExchangeAnnotation) {
            HttpServiceProxyFactory factory = buildFactory(chan);
            T result = (T) factory.createClient(clientType);
            Cache.addClient(result);
            return result;
        }
        if (!httpExchangeProperties.isRequestMappingSupportEnabled()) {
            throw new IllegalStateException(
                    "You're using @RequestMapping based annotation, please migrate to @HttpExchange or set 'http-exchange.request-mapping-support-enabled=true' to support processing @RequestMapping.");
        }
        ShadedHttpServiceProxyFactory shadedFactory = buildShadedFactory(chan);
        T result = (T) shadedFactory.createClient(clientType);
        Cache.addClient(result);
        return result;
    }

    private HttpServiceProxyFactory buildFactory(HttpExchangeProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder builder = Cache.getOrSupply(channelConfig, () -> factoryBuilder(channelConfig));
        return builder.build();
    }

    private ShadedHttpServiceProxyFactory buildShadedFactory(HttpExchangeProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder b = Cache.getOrSupply(channelConfig, () -> factoryBuilder(channelConfig));
        return shadedProxyFactory(b).build();
    }

    private HttpServiceProxyFactory.Builder factoryBuilder(HttpExchangeProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder builder = beanFactory
                .getBeanProvider(HttpServiceProxyFactory.Builder.class)
                .getIfUnique(HttpServiceProxyFactory::builder);

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
                        "Client '{}' contains methods with reactive return types, use client-type '{}' instead of '{}'",
                        clientType.getSimpleName(),
                        WEB_CLIENT,
                        type);
            }
            builder.exchangeAdapter(WebClientAdapter.create(buildWebClient(channelConfig)));
            return;
        }

        switch (getClientType(channelConfig)) {
            case REST_CLIENT -> builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
            case REST_TEMPLATE -> builder.exchangeAdapter(RestTemplateAdapter.create(buildRestTemplate(channelConfig)));
            case WEB_CLIENT -> {
                if (WEBFLUX_PRESENT) {
                    builder.exchangeAdapter(WebClientAdapter.create(buildWebClient(channelConfig)));
                } else {
                    log.warn("spring-webflux is not in the classpath, fall back client-type to '{}'", REST_CLIENT);
                    builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
                }
            }
            default -> throw new IllegalStateException("Unsupported client-type: " + channelConfig.getClientType());
        }
    }

    private void addCustomArgumentResolver(HttpServiceProxyFactory.Builder builder) {
        beanFactory
                .getBeanProvider(HttpServiceArgumentResolver.class)
                .orderedStream()
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
        RestTemplateBuilder builder =
                beanFactory.getBeanProvider(RestTemplateBuilder.class).getIfUnique(RestTemplateBuilder::new);
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder = builder.rootUri(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            for (HttpExchangeProperties.Header header : channelConfig.getHeaders()) {
                builder = builder.defaultHeader(
                        header.getKey(), header.getValues().toArray(String[]::new));
            }
        }
        if (channelConfig.getConnectTimeout() != null) {
            builder = builder.setConnectTimeout(Duration.ofMillis(channelConfig.getConnectTimeout()));
        }
        if (channelConfig.getReadTimeout() != null) {
            builder = builder.setReadTimeout(Duration.ofMillis(channelConfig.getReadTimeout()));
        }
        builder = builder.requestFactory(getRequestFactoryClass(channelConfig));

        RestTemplate restTemplate = builder.build();

        if (isLoadBalancerEnabled(channelConfig)) {
            beanFactory
                    .getBeanProvider(ClientHttpRequestInterceptor.class)
                    .orderedStream()
                    .filter(e -> !restTemplate.getInterceptors().contains(e))
                    .forEach(restTemplate.getInterceptors()::add);
        }

        return restTemplate;
    }

    private WebClient buildWebClient(HttpExchangeProperties.Channel channelConfig) {
        WebClient.Builder builder =
                beanFactory.getBeanProvider(WebClient.Builder.class).getIfUnique(WebClient::builder);
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.baseUrl(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        if (isLoadBalancerEnabled(channelConfig)) {
            builder.filters(it -> beanFactory
                    .getBeanProvider(ExchangeFilterFunction.class)
                    .orderedStream()
                    .filter(ExchangeClientCreator::notLoadBalancedFilter)
                    .filter(e -> !it.contains(e))
                    .forEach(it::add));
        }
        return builder.build();
    }

    private RestClient buildRestClient(HttpExchangeProperties.Channel channelConfig) {
        RestClient.Builder builder =
                beanFactory.getBeanProvider(RestClient.Builder.class).getIfUnique(RestClient::builder);
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.baseUrl(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        builder.requestFactory(getRequestFactory(channelConfig));
        if (isLoadBalancerEnabled(channelConfig)) {
            builder.requestInterceptors(it -> beanFactory
                    .getBeanProvider(ClientHttpRequestInterceptor.class)
                    .orderedStream()
                    .filter(e -> !it.contains(e))
                    .forEach(it::add));
        }
        return builder.build();
    }

    private static boolean notLoadBalancedFilter(ExchangeFilterFunction e) {
        return !LoadBalancedExchangeFilterFunction.class.isAssignableFrom(e.getClass());
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
        return ClientHttpRequestFactories.get(getRequestFactoryClass(channelConfig), settings);
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

    private Class<? extends ClientHttpRequestFactory> getRequestFactoryClass(HttpExchangeProperties.Channel channel) {
        if (RequestConfigurator.class.isAssignableFrom(clientType)) {
            if (channel.getRequestFactory() == null) {
                return EnhancedJdkClientHttpRequestFactory.class;
            }
            if (!Objects.equals(channel.getRequestFactory(), EnhancedJdkClientHttpRequestFactory.class)) {
                log.warn(
                        "Client '{}' extends RequestConfigurator, but request-factory '{}' does not implement RequestConfigurator's features, remove request-factory from configuration or use '{}' instead.",
                        clientType.getSimpleName(),
                        channel.getRequestFactory().getSimpleName(),
                        EnhancedJdkClientHttpRequestFactory.class.getSimpleName());
            }
            return channel.getRequestFactory();
        }
        return JdkClientHttpRequestFactory.class;
    }

    private static HttpExchangeProperties.ClientType getClientType(HttpExchangeProperties.Channel channel) {
        return channel.getClientType() != null ? channel.getClientType() : REST_CLIENT;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, Field field) {
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, obj);
    }
}
