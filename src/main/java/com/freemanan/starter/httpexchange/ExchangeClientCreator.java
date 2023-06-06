package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.httpexchange.Util.findMatchedConfig;

import com.freemanan.starter.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class ExchangeClientCreator {

    private final ConfigurableBeanFactory beanFactory;
    private final Environment environment;
    private final Class<?> clientType;
    private final HttpClientsProperties.Channel channelConfig;
    private final boolean usingClientSideAnnotation;

    ExchangeClientCreator(
            ConfigurableBeanFactory beanFactory,
            HttpClientsProperties properties,
            Class<?> clientType,
            boolean usingClientSideAnnotation) {
        Assert.notNull(beanFactory, "beanFactory must not be null");
        Assert.notNull(properties, "properties must not be null");
        Assert.notNull(clientType, "clientType must not be null");
        this.beanFactory = beanFactory;
        this.environment = beanFactory.getBean(Environment.class);
        this.clientType = clientType;
        this.channelConfig = findMatchedConfig(clientType, properties).orElseGet(properties::defaultClient);
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
        if (usingClientSideAnnotation) {
            HttpServiceProxyFactory cachedFactory = buildServiceProxyFactory();
            T result = (T) cachedFactory.createClient(clientType);
            Cache.addClientClass(clientType);
            return result;
        }
        ShadedHttpServiceProxyFactory cachedFactory = buildShadedServiceProxyFactory();
        T result = (T) cachedFactory.createClient(clientType);
        Cache.addClientClass(clientType);
        return result;
    }

    private HttpServiceProxyFactory buildServiceProxyFactory() {
        HttpServiceProxyFactory.Builder builder = proxyFactoryBuilder();
        return builder.build();
    }

    private ShadedHttpServiceProxyFactory buildShadedServiceProxyFactory() {
        ShadedHttpServiceProxyFactory.Builder builder = ShadedHttpServiceProxyFactory.builder(proxyFactoryBuilder());
        return builder.build();
    }

    private HttpServiceProxyFactory.Builder proxyFactoryBuilder() {
        HttpServiceProxyFactory.Builder builder = beanFactory
                .getBeanProvider(HttpServiceProxyFactory.Builder.class)
                .getIfUnique(HttpServiceProxyFactory::builder)
                .clientAdapter(WebClientAdapter.forClient(buildWebClient()));

        // Customized argument resolvers
        beanFactory
                .getBeanProvider(HttpServiceArgumentResolver.class)
                .orderedStream()
                .forEach(builder::customArgumentResolver);

        // String value resolver, support ${} placeholder by default
        StringValueResolver delegatedResolver = new UrlPlaceholderStringValueResolver(
                environment, beanFactory.getBeanProvider(StringValueResolver.class));
        builder.embeddedValueResolver(delegatedResolver);

        // Response timeout
        Optional.ofNullable(channelConfig.getResponseTimeout())
                .map(Duration::ofMillis)
                .ifPresent(builder::blockTimeout);

        return builder;
    }

    private WebClient buildWebClient() {
        WebClient.Builder builder =
                beanFactory.getBeanProvider(WebClient.Builder.class).getIfUnique(WebClient::builder);
        if (StringUtils.hasText(channelConfig.getBaseUrl())) {
            String baseUrl = channelConfig.getBaseUrl();
            if (!baseUrl.contains("://")) {
                baseUrl = "http://" + baseUrl;
            }
            builder.baseUrl(baseUrl);
        }
        if (!CollectionUtils.isEmpty(channelConfig.getHeaders())) {
            channelConfig
                    .getHeaders()
                    .forEach(header -> builder.defaultHeader(
                            header.getKey(), header.getValues().toArray(String[]::new)));
        }
        return builder.build();
    }
}
