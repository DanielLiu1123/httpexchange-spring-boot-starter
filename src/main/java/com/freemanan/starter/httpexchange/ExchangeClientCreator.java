package com.freemanan.starter.httpexchange;

import com.freemanan.starter.httpexchange.filter.TimeoutExchangeFilter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class ExchangeClientCreator {

    private final ConfigurableBeanFactory beanFactory;
    private final Environment environment;
    private final Class<?> clientType;
    private final HttpClientsProperties.Client client;
    private final ReusableModel model;

    public ExchangeClientCreator(
            ConfigurableBeanFactory beanFactory, HttpClientsProperties properties, Class<?> clientType) {
        Assert.notNull(beanFactory, "beanFactory must not be null");
        Assert.notNull(clientType, "clientType must not be null");
        this.beanFactory = beanFactory;
        this.environment = beanFactory.getBean(Environment.class);
        this.clientType = clientType;
        this.client = getClient(properties, clientType);
        this.model = new ReusableModel(client.getBaseUrl(), client.getResponseTimeout(), client.getHeaders());
    }

    private static HttpClientsProperties.Client getClient(HttpClientsProperties properties, Class<?> type) {
        return properties.getClients().stream()
                .filter(it -> StringUtils.hasText(it.getName()))
                .filter(it -> {
                    String name = it.getName().replaceAll("-", "");
                    return name.equalsIgnoreCase(type.getSimpleName())
                            || name.equalsIgnoreCase(type.getName())
                            || name.equalsIgnoreCase(type.getCanonicalName());
                })
                .findFirst()
                .orElseGet(properties::defaultClient);
    }

    /**
     * Create a proxy {@link HttpExchange} interface instance.
     *
     * @param <T> type of the {@link HttpExchange} interface
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        HttpServiceProxyFactory cachedFactory = getOrCreateServiceProxyFactory();
        return (T) cachedFactory.createClient(clientType);
    }

    private HttpServiceProxyFactory getOrCreateServiceProxyFactory() {
        return Cache.getFactory(model, this::buildServiceProxyFactory);
    }

    private HttpServiceProxyFactory buildServiceProxyFactory() {
        HttpServiceProxyFactory.Builder builder = beanFactory
                .getBeanProvider(HttpServiceProxyFactory.Builder.class)
                .getIfUnique(HttpServiceProxyFactory::builder)
                .clientAdapter(WebClientAdapter.forClient(getOrCreateWebClient()))
                .embeddedValueResolver(environment::resolvePlaceholders);
        return builder.build();
    }

    private WebClient getOrCreateWebClient() {
        return Cache.getWebClient(model, this::buildWebClient);
    }

    private WebClient buildWebClient() {
        WebClient.Builder builder =
                beanFactory.getBeanProvider(WebClient.Builder.class).getIfUnique(WebClient::builder);
        if (client.getBaseUrl() != null) {
            builder.baseUrl(client.getBaseUrl());
        }
        if (client.getResponseTimeout() != null) {
            builder.filter(new TimeoutExchangeFilter(client.getResponseTimeout()));
        }
        if (!CollectionUtils.isEmpty(client.getHeaders())) {
            client.getHeaders().forEach((key, values) -> builder.defaultHeader(key, values.toArray(new String[0])));
        }
        return builder.build();
    }
}
