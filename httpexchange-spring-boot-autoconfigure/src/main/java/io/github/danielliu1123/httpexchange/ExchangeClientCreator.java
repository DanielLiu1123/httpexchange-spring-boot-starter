package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType.REST_CLIENT;
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
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
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
        switch (getClientType(channelConfig)) {
            case REST_CLIENT -> builder.exchangeAdapter(RestClientAdapter.create(
                    getClient(new Cache.ClientId(channelConfig, REST_CLIENT), () -> buildRestClient(channelConfig))));
            case WEB_CLIENT -> builder.exchangeAdapter(WebClientAdapter.create(
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

    private WebClient buildWebClient(HttpExchangeProperties.Channel channelConfig) {
        WebClient.Builder builder = WebClient.builder();

        configureWebClientBuilder(builder, channelConfig);

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

        var settings = buildSettingsForWebClient(channelConfig);

        builder.clientConnector(clientConnectorBuilder.build(settings));
    }

    private RestClient buildRestClient(HttpExchangeProperties.Channel channelConfig) {
        // Do not use RestClient.Builder bean here, because we can't know requestFactory is configured by user or not
        RestClient.Builder builder = RestClient.builder();

        configureRestClientBuilder(builder, channelConfig);

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

        var settings = buildSettingsForRestClient(channelConfig);

        builder.requestFactory(requestFactoryBuilder.build(settings));

        var customizers = beanFactory
                .getBeanProvider(RestClientCustomizer.class)
                .orderedStream()
                .toList();

        for (var customizer : customizers) {
            customizer.customize(builder);
        }
    }

    private ClientHttpRequestFactorySettings buildSettingsForRestClient(HttpExchangeProperties.Channel channelConfig) {

        var globalConfig = beanFactory
                .getBeanProvider(ClientHttpRequestFactorySettings.class)
                .getIfUnique(ClientHttpRequestFactorySettings::defaults);

        var redirects = Optional.ofNullable(channelConfig.getRedirects()).orElseGet(globalConfig::redirects);
        var connectTimeout = Optional.ofNullable(channelConfig.getConnectTimeout())
                .map(Duration::ofMillis)
                .orElseGet(globalConfig::connectTimeout);
        var readTimeout = Optional.ofNullable(channelConfig.getReadTimeout())
                .map(Duration::ofMillis)
                .orElseGet(globalConfig::readTimeout);
        var sslBundle = Optional.ofNullable(channelConfig.getSsl())
                .map(HttpExchangeProperties.Ssl::getBundle)
                .filter(StringUtils::hasText)
                .map(bundle -> beanFactory.getBean(SslBundles.class).getBundle(bundle))
                .orElseGet(globalConfig::sslBundle);

        return new ClientHttpRequestFactorySettings(redirects, connectTimeout, readTimeout, sslBundle);
    }

    private ClientHttpConnectorSettings buildSettingsForWebClient(HttpExchangeProperties.Channel channelConfig) {

        var globalConfig = beanFactory
                .getBeanProvider(ClientHttpConnectorSettings.class)
                .getIfUnique(ClientHttpConnectorSettings::defaults);

        var redirects = Optional.ofNullable(channelConfig.getRedirects()).orElseGet(globalConfig::redirects);
        var connectTimeout = Optional.ofNullable(channelConfig.getConnectTimeout())
                .map(Duration::ofMillis)
                .orElseGet(globalConfig::connectTimeout);
        var readTimeout = Optional.ofNullable(channelConfig.getReadTimeout())
                .map(Duration::ofMillis)
                .orElseGet(globalConfig::readTimeout);
        var sslBundle = Optional.ofNullable(channelConfig.getSsl())
                .map(HttpExchangeProperties.Ssl::getBundle)
                .filter(StringUtils::hasText)
                .map(bundle -> beanFactory.getBean(SslBundles.class).getBundle(bundle))
                .orElseGet(globalConfig::sslBundle);

        return new ClientHttpConnectorSettings(redirects, connectTimeout, readTimeout, sslBundle);
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

    private HttpExchangeProperties.ClientType getClientType(HttpExchangeProperties.Channel channel) {

        if (WEBFLUX_PRESENT && hasReactiveReturnTypeMethod(clientType)) {
            HttpExchangeProperties.ClientType type = channel.getClientType();
            if (type != null && type != WEB_CLIENT) {
                log.warn(
                        "{} contains methods with reactive return types, should use the client-type '{}' instead of '{}'",
                        clientType.getSimpleName(),
                        WEB_CLIENT,
                        type);
            }
            return WEB_CLIENT;
        }

        var configured = channel.getClientType() != null ? channel.getClientType() : getDefaultClientType();

        if (configured == WEB_CLIENT && !WEBFLUX_PRESENT) {
            log.warn(
                    "Since spring-webflux is not in the classpath, the client-type will fall back to '{}'",
                    getDefaultClientType());
            return getDefaultClientType();
        }

        return configured;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, Field field) {
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, obj);
    }

    private static HttpExchangeProperties.ClientType getDefaultClientType() {
        return REST_CLIENT;
    }
}
