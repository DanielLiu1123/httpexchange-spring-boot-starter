package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.REST_CLIENT;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.WEB_CLIENT;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class ExchangeClientCreator {
    private static final Logger log = LoggerFactory.getLogger(ExchangeClientCreator.class);

    private static final boolean LOADBALANCER_PRESENT =
            ClassUtils.isPresent("org.springframework.cloud.client.loadbalancer.LoadBalancerClient", null);
    private static final boolean DEFERRING_LOADBALANCER_INTERCEPTOR_PRESENT = ClassUtils.isPresent(
            "org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor", null);
    private static final boolean springBootStarterRestClientPresent =
            ClassUtils.isPresent("org.springframework.boot.restclient.RestClientCustomizer", null);
    private static final boolean springBootStarterWebClientPresent =
            ClassUtils.isPresent("org.springframework.boot.webclient.WebClientCustomizer", null);

    private static final Field exchangeAdapterField;
    private static final Field customArgumentResolversField;
    private static final Field conversionServiceField;
    private static final Field embeddedValueResolverField;
    private static final Field requestValuesProcessorsField;
    private static final Field exchangeAdapterDecoratorField;

    static {
        try {
            Class<HttpServiceProxyFactory.Builder> clz = HttpServiceProxyFactory.Builder.class;
            exchangeAdapterField = clz.getDeclaredField("exchangeAdapter");
            customArgumentResolversField = clz.getDeclaredField("customArgumentResolvers");
            conversionServiceField = clz.getDeclaredField("conversionService");
            embeddedValueResolverField = clz.getDeclaredField("embeddedValueResolver");
            requestValuesProcessorsField = clz.getDeclaredField("requestValuesProcessors"); // From Spring 7.x
            exchangeAdapterDecoratorField = clz.getDeclaredField("exchangeAdapterDecorator"); // From Spring 7.x
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
        HttpExchangeProperties properties = beanFactory
                .getBeanProvider(HttpExchangeProperties.class)
                .getIfUnique(() -> Util.getProperties(environment));
        HttpExchangeProperties.Channel chan = getMatchedConfig(clientType, properties);
        if (isUseHttpExchangeAnnotation) {
            HttpServiceProxyFactory factory = factoryBuilder(chan).build();
            T result = (T) factory.createClient(clientType);
            Cache.addClient(result);
            return result;
        }
        if (!properties.isRequestMappingSupportEnabled()) {
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

    private HttpExchangeProperties.Channel getMatchedConfig(Class<?> clientType, HttpExchangeProperties properties) {
        List<HttpExchangeProperties.Channel> matchedConfigs = Util.findMatchedConfigs(clientType, properties);
        if (matchedConfigs.isEmpty()) {
            return properties.defaultChannel();
        }
        if (matchedConfigs.size() > 1) {
            String matchedNames = matchedConfigs.stream()
                    .map(it -> it.getName() != null ? it.getName() : "unnamed")
                    .collect(Collectors.joining(", "));
            var chosen = matchedConfigs.get(0);
            log.warn(
                    "Exchange client [{}] matched multiple channels: [{}], using '{}' with base-url '{}'",
                    clientType.getName(),
                    matchedNames,
                    chosen.getName() != null ? chosen.getName() : "unnamed",
                    chosen.getBaseUrl() != null ? chosen.getBaseUrl() : properties.getBaseUrl());
        }
        return matchedConfigs.get(0);
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
        switch (getClientType(channelConfig)) {
            case REST_CLIENT ->
                builder.exchangeAdapter(RestClientAdapter.create(getClient(
                        new Cache.ClientId(channelConfig, REST_CLIENT), () -> buildRestClient(channelConfig))));
            case WEB_CLIENT ->
                builder.exchangeAdapter(WebClientAdapter.create(
                        getClient(new Cache.ClientId(channelConfig, WEB_CLIENT), () -> buildWebClient(channelConfig))));
            default -> throw new IllegalStateException("Unsupported client-type: " + channelConfig.getClientType());
        }
    }

    private static <T> T getClient(Cache.ClientId clientId, Supplier<T> supplier) {
        return Boolean.TRUE.equals(clientId.channel().getHttpClientReuseEnabled())
                ? Cache.getHttpClient(clientId, supplier)
                : supplier.get();
    }

    private void addCustomArgumentResolver(HttpServiceProxyFactory.Builder builder) {
        List<HttpServiceArgumentResolver> existingResolvers = Optional.<List<HttpServiceArgumentResolver>>ofNullable(
                        getFieldValue(builder, customArgumentResolversField))
                .orElseGet(List::of);
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
                .map(r -> new UrlPlaceholderStringValueResolver(environment, r))
                .orElseGet(() -> new UrlPlaceholderStringValueResolver(environment, null));
        builder.embeddedValueResolver(resolver);
    }

    private WebClient buildWebClient(HttpExchangeProperties.Channel channelConfig) {
        WebClient.Builder builder = WebClient.builder();

        configureWebClientBuilder(builder, channelConfig);

        var baseUrl = channelConfig.getBaseUrl();
        if (StringUtils.hasText(baseUrl)) {
            builder.baseUrl(getRealBaseUrl(baseUrl));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header ->
                            builder.defaultHeader(header.key(), header.values().toArray(String[]::new)));
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

    private void configureWebClientBuilder(WebClient.Builder builder, HttpExchangeProperties.Channel channelConfig) {

        var customizers = beanFactory
                .getBeanProvider(WebClientCustomizer.class)
                .orderedStream()
                .toList();

        // Invoke customizers will set clientConnector,
        // but it's OK, we will override it later
        for (var customizer : customizers) {
            customizer.customize(builder);
        }

        var clientConnectorBuilder = beanFactory
                .getBeanProvider(ClientHttpConnectorBuilder.class)
                .getIfUnique(ClientHttpConnectorBuilder::detect);

        var settings = buildHttpClientSettings(channelConfig);

        builder.clientConnector(clientConnectorBuilder.build(settings));
    }

    private RestClient buildRestClient(HttpExchangeProperties.Channel channelConfig) {
        // Do not use RestClient.Builder bean here, because we can't know requestFactory is configured by user or not
        RestClient.Builder builder = RestClient.builder();

        configureRestClientBuilder(builder, channelConfig);

        var baseUrl = channelConfig.getBaseUrl();
        if (StringUtils.hasText(baseUrl)) {
            builder.baseUrl(getRealBaseUrl(baseUrl));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header ->
                            builder.defaultHeader(header.key(), header.values().toArray(String[]::new)));
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

    private void configureRestClientBuilder(RestClient.Builder builder, HttpExchangeProperties.Channel channelConfig) {

        // see RestClientBuilderConfigurer
        // see org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.restClientBuilder

        var requestFactoryBuilder = beanFactory
                .getBeanProvider(ClientHttpRequestFactoryBuilder.class)
                .getIfUnique(ClientHttpRequestFactoryBuilder::detect);

        var settings = buildHttpClientSettings(channelConfig);

        builder.requestFactory(requestFactoryBuilder.build(settings));

        var customizers = beanFactory
                .getBeanProvider(RestClientCustomizer.class)
                .orderedStream()
                .toList();

        for (var customizer : customizers) {
            customizer.customize(builder);
        }
    }

    private HttpClientSettings buildHttpClientSettings(HttpExchangeProperties.Channel channelConfig) {

        var globalConfig =
                beanFactory.getBeanProvider(HttpClientSettings.class).getIfUnique(HttpClientSettings::defaults);

        var redirects = Optional.ofNullable(channelConfig.getRedirects()).orElseGet(globalConfig::redirects);
        var connectTimeout = Optional.ofNullable(channelConfig.getConnectTimeout())
                .map(Duration::ofMillis)
                .orElseGet(globalConfig::connectTimeout);
        var readTimeout = Optional.ofNullable(channelConfig.getReadTimeout())
                .map(Duration::ofMillis)
                .orElseGet(globalConfig::readTimeout);
        var sslBundle = Optional.ofNullable(channelConfig.getSsl())
                .map(HttpExchangeProperties.Ssl::bundle)
                .filter(StringUtils::hasText)
                .map(bundle -> beanFactory.getBean(SslBundles.class).getBundle(bundle))
                .orElseGet(globalConfig::sslBundle);

        return new HttpClientSettings(redirects, connectTimeout, readTimeout, sslBundle);
    }

    private boolean isLoadBalancerEnabled(HttpExchangeProperties.Channel channelConfig) {
        return LOADBALANCER_PRESENT
                && environment.getProperty("spring.cloud.loadbalancer.enabled", Boolean.class, true)
                && Boolean.TRUE.equals(channelConfig.getLoadbalancerEnabled());
    }

    private static String getRealBaseUrl(String baseUrl) {
        return baseUrl.contains("://") ? baseUrl : "http://" + baseUrl;
    }

    static ShadedHttpServiceProxyFactory.Builder shadedProxyFactory(HttpServiceProxyFactory.Builder proxyFactory) {
        HttpExchangeAdapter exchangeAdapter = getFieldValue(proxyFactory, exchangeAdapterField);
        List<HttpServiceArgumentResolver> customArgumentResolvers =
                getFieldValue(proxyFactory, customArgumentResolversField);
        ConversionService conversionService = getFieldValue(proxyFactory, conversionServiceField);
        StringValueResolver embeddedValueResolver = getFieldValue(proxyFactory, embeddedValueResolverField);
        List<HttpRequestValues.Processor> requestValuesProcessors =
                getFieldValue(proxyFactory, requestValuesProcessorsField);
        Function<HttpExchangeAdapter, HttpExchangeAdapter> exchangeAdapterDecorator =
                getFieldValue(proxyFactory, exchangeAdapterDecoratorField);

        ShadedHttpServiceProxyFactory.Builder builder = ShadedHttpServiceProxyFactory.builder();
        Optional.ofNullable(exchangeAdapter).ifPresent(builder::exchangeAdapter);
        Optional.ofNullable(customArgumentResolvers).stream()
                .flatMap(Collection::stream)
                .forEach(builder::customArgumentResolver);
        Optional.ofNullable(conversionService).ifPresent(builder::conversionService);
        Optional.ofNullable(embeddedValueResolver).ifPresent(builder::embeddedValueResolver);
        Optional.ofNullable(requestValuesProcessors).stream()
                .flatMap(Collection::stream)
                .forEach(builder::httpRequestValuesProcessor);
        Optional.ofNullable(exchangeAdapterDecorator).ifPresent(builder::exchangeAdapterDecorator);
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

    private HttpExchangeProperties.ClientType getClientType(HttpExchangeProperties.Channel channel) {
        var type = channel.getClientType() != null ? channel.getClientType() : getDefaultClientType();
        return switch (type) {
            case REST_CLIENT -> {
                if (!springBootStarterRestClientPresent) {
                    throw new IllegalStateException(
                            "You need to add 'spring-boot-starter-restclient' to the classpath to use REST_CLIENT");
                }
                if (springBootStarterWebClientPresent && hasReactiveReturnTypeMethod(clientType)) {
                    log.warn(
                            "{} contains methods with reactive return types, should use the client-type '{}' instead of '{}'",
                            clientType.getSimpleName(),
                            WEB_CLIENT,
                            REST_CLIENT);
                    yield WEB_CLIENT;
                }
                yield REST_CLIENT;
            }
            case WEB_CLIENT -> {
                if (!springBootStarterWebClientPresent) {
                    throw new IllegalStateException(
                            "You need to add 'spring-boot-starter-webclient' to the classpath to use WEB_CLIENT");
                }
                yield WEB_CLIENT;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T getFieldValue(Object obj, Field field) {
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, obj);
    }

    private static HttpExchangeProperties.ClientType getDefaultClientType() {
        if (springBootStarterRestClientPresent) {
            return REST_CLIENT;
        }
        if (springBootStarterWebClientPresent) {
            return WEB_CLIENT;
        }
        throw new IllegalStateException(
                "You need to add 'spring-boot-starter-restclient' or 'spring-boot-starter-webclient' to the classpath");
    }
}
