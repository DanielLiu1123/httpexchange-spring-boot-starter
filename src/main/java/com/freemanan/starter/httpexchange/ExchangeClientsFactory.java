package com.freemanan.starter.httpexchange;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
public class ExchangeClientsFactory {

    private final ConfigurableBeanFactory beanFactory;
    private final Class<?> type;

    public ExchangeClientsFactory(ConfigurableBeanFactory beanFactory, Class<?> type) {
        Assert.notNull(beanFactory, "beanFactory must not be null");
        Assert.notNull(type, "type must not be null");
        this.beanFactory = beanFactory;
        this.type = type;
    }

    /**
     * Create a proxy {@link HttpExchange} interface instance.
     *
     * @param <T> type of the {@link HttpExchange} interface
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T create() {
        HttpServiceProxyFactory.Builder builder = beanFactory
                .getBeanProvider(HttpServiceProxyFactory.Builder.class)
                .getIfUnique();
        // look up from beanFactory
        // 1. look up HttpServiceProxyFactory.Builder first
        if (builder != null) {
            return (T) builder
                    // support url placeholder '${}'
                    .embeddedValueResolver(beanFactory.getBean(Environment.class)::resolvePlaceholders)
                    .build()
                    .createClient(type);
        }
        // 2. look up HttpServiceProxyFactory
        HttpServiceProxyFactory factory =
                beanFactory.getBeanProvider(HttpServiceProxyFactory.class).getIfUnique();
        if (factory != null) {
            // If HttpServiceProxyFactory is created by user
            // may not support url placeholder '${}'
            return (T) factory.createClient(type);
        }

        // look up from holder, don't add to beanFactory
        HttpServiceProxyFactory cachedFactory =
                Holder.getOrSupply(() -> HttpServiceProxyFactory.builder(WebClientAdapter.forClient(beanFactory
                                .getBeanProvider(WebClient.Builder.class)
                                .getIfUnique(WebClient::builder)
                                .build()))
                        // support url placeholder '${}'
                        .embeddedValueResolver(beanFactory.getBean(Environment.class)::resolvePlaceholders)
                        .build());
        return (T) cachedFactory.createClient(type);
    }

    private static class Holder {
        private static final AtomicReference<HttpServiceProxyFactory> INSTANCE = new AtomicReference<>();

        public static synchronized HttpServiceProxyFactory getOrSupply(Supplier<HttpServiceProxyFactory> supplier) {
            if (INSTANCE.get() == null) {
                INSTANCE.set(supplier.get());
            }
            return INSTANCE.get();
        }
    }
}
