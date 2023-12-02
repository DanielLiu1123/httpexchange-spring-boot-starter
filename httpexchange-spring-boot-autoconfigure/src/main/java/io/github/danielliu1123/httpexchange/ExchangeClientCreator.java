package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Util.findMatchedConfig;

import io.github.danielliu1123.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
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
    private static final boolean SPRING_RETRY_PRESENT =
            ClassUtils.isPresent("org.springframework.retry.support.RetryTemplate", null);

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
    private final boolean usingNeutralAnnotation;

    ExchangeClientCreator(ConfigurableBeanFactory beanFactory, Class<?> clientType, boolean usingNeutralAnnotation) {
        this.beanFactory = beanFactory;
        this.environment = beanFactory.getBean(Environment.class);
        this.clientType = clientType;
        this.usingNeutralAnnotation = usingNeutralAnnotation;
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
        if (usingNeutralAnnotation) {
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

        HttpExchangeAdapter exchangeAdapter = getFieldValue(builder, exchangeAdapterField);
        if (exchangeAdapter == null) {
            switch (channelConfig.getClientType()) {
                case REST_CLIENT -> builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
                case REST_TEMPLATE -> builder.exchangeAdapter(
                        RestTemplateAdapter.create(buildRestTemplate(channelConfig)));
                case WEB_CLIENT -> {
                    if (WEBFLUX_PRESENT) {
                        builder.exchangeAdapter(WebClientAdapter.create(buildWebClient(channelConfig)));
                    } else {
                        log.warn(
                                "spring-webflux is not in the classpath, fall back client-type to '{}'",
                                ClientType.REST_CLIENT);
                        builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + channelConfig.getClientType());
            }
        }

        // String value resolver, need to support ${} placeholder
        StringValueResolver resolver = Optional.ofNullable(getFieldValue(builder, embeddedValueResolverField))
                .map(StringValueResolver.class::cast)
                .map(r -> UrlPlaceholderStringValueResolver.create(environment, r))
                .orElseGet(() -> UrlPlaceholderStringValueResolver.create(environment, null));
        builder.embeddedValueResolver(resolver);

        // custom HttpServiceArgumentResolver
        beanFactory
                .getBeanProvider(HttpServiceArgumentResolver.class)
                .orderedStream()
                .forEach(builder::customArgumentResolver);

        return builder;
    }

    private RestTemplate buildRestTemplate(HttpExchangeProperties.Channel channelConfig) {
        RestTemplateBuilder builder =
                beanFactory.getBeanProvider(RestTemplateBuilder.class).getIfUnique(RestTemplateBuilder::new);
        // see org.springframework.boot.web.client.RestTemplateBuilder#rootUri
        String rootUri = getFieldValue(builder, "rootUri");
        if (!StringUtils.hasText(rootUri) && StringUtils.hasText(channelConfig.getBaseUrl())) {
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
        builder = builder.requestFactory(() -> getRequestFactory(channelConfig));

        RestTemplate restTemplate = builder.build();

        if (isLoadBalancerEnabled(channelConfig)) {
            if (SPRING_RETRY_PRESENT && isLoadBalancerRetryEnabled()) {
                RetryLoadBalancerInterceptor retryInterceptor = beanFactory
                        .getBeanProvider(RetryLoadBalancerInterceptor.class)
                        .getIfUnique();
                if (retryInterceptor != null) {
                    addIfAbsent(restTemplate, retryInterceptor);
                } else {
                    log.warn(
                            "Not found bean of type 'RetryLoadBalancerInterceptor', fallback to 'LoadBalancerInterceptor'");
                    addIfAbsent(restTemplate, beanFactory.getBean(LoadBalancerInterceptor.class));
                }
            } else {
                addIfAbsent(restTemplate, beanFactory.getBean(LoadBalancerInterceptor.class));
            }
        }

        return restTemplate;
    }

    private WebClient buildWebClient(HttpExchangeProperties.Channel channelConfig) {
        WebClient.Builder builder =
                beanFactory.getBeanProvider(WebClient.Builder.class).getIfUnique(WebClient::builder);
        // see org.springframework.web.reactive.function.client.DefaultWebClientBuilder#baseUrl
        String baseUrl = getFieldValue(builder, "baseUrl");
        if (!StringUtils.hasText(baseUrl) && StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.baseUrl(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        if (isLoadBalancerEnabled(channelConfig)) {
            builder.filters(
                    filters -> Optional.of(beanFactory.getBean(DeferringLoadBalancerExchangeFilterFunction.class))
                            .filter(f -> !filters.contains(f))
                            .ifPresent(filters::add));
        }
        return builder.build();
    }

    private RestClient buildRestClient(HttpExchangeProperties.Channel channelConfig) {
        RestClient.Builder builder =
                beanFactory.getBeanProvider(RestClient.Builder.class).getIfUnique(RestClient::builder);
        // see org.springframework.web.client.DefaultRestClientBuilder#baseUrl
        String baseUrl = getFieldValue(builder, "baseUrl");
        if (!StringUtils.hasText(baseUrl) && StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.baseUrl(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        builder.requestFactory(getRequestFactory(channelConfig));
        // If loadbalancer in the classpath, use LoadBalancerInterceptor.
        if (isLoadBalancerEnabled(channelConfig)) {
            if (SPRING_RETRY_PRESENT && isLoadBalancerRetryEnabled()) {
                RetryLoadBalancerInterceptor retryInterceptor = beanFactory
                        .getBeanProvider(RetryLoadBalancerInterceptor.class)
                        .getIfUnique();
                if (retryInterceptor != null) {
                    addIfAbsent(builder, retryInterceptor);
                } else {
                    log.warn(
                            "Not found bean of type 'RetryLoadBalancerInterceptor', fallback to 'LoadBalancerInterceptor'");
                    addIfAbsent(builder, beanFactory.getBean(LoadBalancerInterceptor.class));
                }
            } else {
                addIfAbsent(builder, beanFactory.getBean(LoadBalancerInterceptor.class));
            }
        }
        return builder.build();
    }

    private static void addIfAbsent(RestClient.Builder builder, ClientHttpRequestInterceptor retryInterceptor) {
        builder.requestInterceptors(interceptors -> Optional.of(retryInterceptor)
                .filter(f -> !interceptors.contains(f))
                .ifPresent(interceptors::add));
    }

    private static void addIfAbsent(RestTemplate restTemplate, ClientHttpRequestInterceptor retryInterceptor) {
        if (!restTemplate.getInterceptors().contains(retryInterceptor)) {
            restTemplate.getInterceptors().add(retryInterceptor);
        }
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
        ClientHttpRequestFactory requestFactory =
                beanFactory.getBeanProvider(ClientHttpRequestFactory.class).getIfUnique();
        return requestFactory != null
                ? ClientHttpRequestFactories.get(
                        AopProxyUtils.ultimateTargetClass(requestFactory).asSubclass(ClientHttpRequestFactory.class),
                        settings)
                : ClientHttpRequestFactories.get(JdkClientHttpRequestFactory.class, settings);
    }

    private boolean isLoadBalancerEnabled(HttpExchangeProperties.Channel channelConfig) {
        return LOADBALANCER_PRESENT
                && environment.getProperty("spring.cloud.loadbalancer.enabled", Boolean.class, true)
                && channelConfig.getLoadBalancerEnabled();
    }

    private boolean isLoadBalancerRetryEnabled() {
        return environment.getProperty("spring.cloud.loadbalancer.retry.enabled", Boolean.class, true);
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

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, Field field) {
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, obj);
    }

    private static <T> T getFieldValue(Object obj, String fieldName) {
        Field field = ReflectionUtils.findField(obj.getClass(), fieldName);
        Assert.notNull(field, "No such field '" + fieldName + "' in " + obj.getClass());
        return getFieldValue(obj, field);
    }
}
