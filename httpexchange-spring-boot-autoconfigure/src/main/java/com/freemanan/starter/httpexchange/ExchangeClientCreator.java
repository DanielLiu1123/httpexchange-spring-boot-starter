package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.httpexchange.Util.findMatchedConfig;

import com.freemanan.starter.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
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

    private static final boolean REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Mono", null);

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
        HttpClientsProperties httpClientsProperties = beanFactory
                .getBeanProvider(HttpClientsProperties.class)
                .getIfUnique(() -> Util.getProperties(environment));
        HttpClientsProperties.Channel chan =
                findMatchedConfig(clientType, httpClientsProperties).orElseGet(httpClientsProperties::defaultClient);
        if (usingNeutralAnnotation) {
            HttpServiceProxyFactory factory = buildFactory(chan);
            T result = (T) factory.createClient(clientType);
            Cache.addClient(result);
            return result;
        }
        if (!httpClientsProperties.isRequestMappingSupportEnabled()) {
            throw new IllegalStateException(
                    "You're using @RequestMapping based annotation, please migrate to @HttpExchange or set 'http-exchange.support-request-mapping=true' to support processing @RequestMapping.");
        }
        ShadedHttpServiceProxyFactory shadedFactory = buildShadedFactory(chan);
        T result = (T) shadedFactory.createClient(clientType);
        Cache.addClient(result);
        return result;
    }

    private HttpServiceProxyFactory buildFactory(HttpClientsProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder builder = factoryBuilder(channelConfig);
        return builder.build();
    }

    private ShadedHttpServiceProxyFactory buildShadedFactory(HttpClientsProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder b = factoryBuilder(channelConfig);
        return shadedProxyFactory(b).build();
    }

    private HttpServiceProxyFactory.Builder factoryBuilder(HttpClientsProperties.Channel channelConfig) {
        HttpServiceProxyFactory.Builder builder = beanFactory
                .getBeanProvider(HttpServiceProxyFactory.Builder.class)
                .getIfUnique(HttpServiceProxyFactory::builder);

        HttpExchangeAdapter exchangeAdapter = getFieldValue(builder, exchangeAdapterField);
        if (exchangeAdapter == null) {
            switch (channelConfig.getBackend()) {
                case REST_CLIENT -> builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
                case REST_TEMPLATE -> builder.exchangeAdapter(
                        RestTemplateAdapter.create(buildRestTemplate(channelConfig)));
                case WEB_CLIENT -> {
                    if (REACTOR_PRESENT) {
                        builder.exchangeAdapter(WebClientAdapter.create(buildWebClient(channelConfig)));
                    } else {
                        log.warn(
                                "Reactor is not present, fall back backends to {}",
                                HttpClientsProperties.Backend.REST_CLIENT.name());
                        builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + channelConfig.getBackend());
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

    private RestTemplate buildRestTemplate(HttpClientsProperties.Channel channelConfig) {
        RestTemplateBuilder builder =
                beanFactory.getBeanProvider(RestTemplateBuilder.class).getIfUnique(RestTemplateBuilder::new);
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            builder.rootUri(getRealBaseUrl(channelConfig));
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        return builder.build();
    }

    private WebClient buildWebClient(HttpClientsProperties.Channel channelConfig) {
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
        return builder.build();
    }

    private RestClient buildRestClient(HttpClientsProperties.Channel channelConfig) {
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
        return builder.build();
    }

    private static String getRealBaseUrl(HttpClientsProperties.Channel channelConfig) {
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
}
