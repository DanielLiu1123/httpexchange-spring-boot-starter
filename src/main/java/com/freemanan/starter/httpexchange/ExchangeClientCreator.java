package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.httpexchange.Util.findMatchedConfig;

import com.freemanan.starter.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
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

    private final ConfigurableBeanFactory beanFactory;
    private final Environment environment;
    private final Class<?> clientType;
    private final boolean usingClientSideAnnotation;

    ExchangeClientCreator(ConfigurableBeanFactory beanFactory, Class<?> clientType, boolean usingClientSideAnnotation) {
        this.beanFactory = beanFactory;
        this.environment = beanFactory.getBean(Environment.class);
        this.clientType = clientType;
        this.usingClientSideAnnotation = usingClientSideAnnotation;
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
        if (usingClientSideAnnotation) {
            HttpServiceProxyFactory factory = buildFactory(chan);
            T result = (T) factory.createClient(clientType);
            Cache.addClient(result);
            return result;
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

        switch (channelConfig.getBackend()) {
            case WEB_CLIENT -> builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
            case REST_CLIENT -> {
                if (REACTOR_PRESENT) {
                    builder.exchangeAdapter(WebClientAdapter.forClient(buildWebClient(channelConfig)));
                } else {
                    log.warn("Reactor is not present, fall back backends to REST_CLIENT");
                    builder.exchangeAdapter(RestClientAdapter.create(buildRestClient(channelConfig)));
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + channelConfig.getBackend());
        }

        // String value resolver, support ${} placeholder by default
        StringValueResolver resolver = Optional.ofNullable(getFieldValue(builder, "embeddedValueResolver"))
                .map(StringValueResolver.class::cast)
                .orElseGet(() -> UrlPlaceholderStringValueResolver.create(environment, null));
        builder.embeddedValueResolver(resolver);

        // Response timeout
        Optional.ofNullable(channelConfig.getResponseTimeout())
                .map(Duration::ofMillis)
                .ifPresent(builder::blockTimeout);

        return builder;
    }

    private WebClient buildWebClient(HttpClientsProperties.Channel channelConfig) {
        WebClient.Builder builder =
                beanFactory.getBeanProvider(WebClient.Builder.class).getIfUnique(WebClient::builder);
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            String baseUrl = channelConfig.getBaseUrl();
            if (!baseUrl.contains("://")) {
                baseUrl = "http://" + baseUrl;
            }
            builder.baseUrl(baseUrl);
        } else {
            log.warn("No base-url configuration found for client: {}", clientType.getName());
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
            String baseUrl = channelConfig.getBaseUrl();
            if (!baseUrl.contains("://")) {
                baseUrl = "http://" + baseUrl;
            }
            builder.baseUrl(baseUrl);
        } else {
            log.warn("No base-url configuration found for client: {}", clientType.getName());
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        return builder.build();
    }

    private static ShadedHttpServiceProxyFactory.Builder shadedProxyFactory(
            HttpServiceProxyFactory.Builder proxyFactory) {
        HttpExchangeAdapter exchangeAdapter = getFieldValue(proxyFactory, "exchangeAdapter");
        List<HttpServiceArgumentResolver> customArgumentResolvers =
                getFieldValue(proxyFactory, "customArgumentResolvers");
        ConversionService conversionService = getFieldValue(proxyFactory, "conversionService");
        StringValueResolver embeddedValueResolver = getFieldValue(proxyFactory, "embeddedValueResolver");

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
    private static <T> T getFieldValue(Object obj, String fieldName) {
        Field field = ReflectionUtils.findField(obj.getClass(), fieldName);
        Assert.notNull(field, "Field '" + fieldName + "' not found");
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, obj);
    }
}
