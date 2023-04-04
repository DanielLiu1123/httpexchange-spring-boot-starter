package com.freemanan.starter.httpexchange;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.BeanFactory;
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
        // look up from bean factory
        // 1. look up HttpServiceProxyFactory first
        HttpServiceProxyFactory factory =
                beanFactory.getBeanProvider(HttpServiceProxyFactory.class).getIfUnique();
        if (factory != null) {
            // If HttpServiceProxyFactory is created by user
            // may not support url placeholder '${}'
            return (T) factory.createClient(type);
        }

        // 2. look up HttpServiceProxyFactory.Builder
        // NOTE: HttpServiceProxyFactory.Builder is a prototype bean, we need to cache it
        // see https://github.com/spring-projects/spring-boot/issues/31337
        // see https://github.com/spring-projects/spring-framework/issues/29296
        HttpServiceProxyFactory.Builder builder =
                Holder.getOrSupply(beanFactory, HttpServiceProxyFactory.Builder.class, () -> beanFactory
                        .getBeanProvider(HttpServiceProxyFactory.Builder.class)
                        .getIfUnique());
        if (builder != null) {
            return (T) builder
                    // support url placeholder '${}'
                    .embeddedValueResolver(beanFactory.getBean(Environment.class)::resolvePlaceholders)
                    .build()
                    .createClient(type);
        }

        // look up from holder, don't add to beanFactory
        HttpServiceProxyFactory cachedFactory =
                Holder.getOrSupply(beanFactory, HttpServiceProxyFactory.class, () -> HttpServiceProxyFactory.builder(
                                WebClientAdapter.forClient(getWebClient()))
                        // support url placeholder '${}'
                        .embeddedValueResolver(beanFactory.getBean(Environment.class)::resolvePlaceholders)
                        .build());
        Assert.notNull(cachedFactory, "cachedFactory must not be null");
        return (T) cachedFactory.createClient(type);
    }

    private WebClient getWebClient() {
        // lookup existing WebClient bean
        WebClient webClientBean = beanFactory.getBeanProvider(WebClient.class).getIfUnique();
        if (webClientBean != null) {
            return webClientBean;
        }
        // lookup existing WebClient.Builder bean, create WebClient bean
        return beanFactory
                .getBeanProvider(WebClient.Builder.class)
                .getIfUnique(WebClient::builder)
                .build();
    }

    private static class Holder {
        private static final Object NULL = new Object();
        /**
         * Use BeanFactory as the first-level cache, because in the tests, the Holder will be reused, causing the cache has unexpected behavior.
         */
        private static final Map<BeanFactory, Map<Class<?>, Object>> cache = new IdentityHashMap<>();

        public static <T> T getOrSupply(BeanFactory beanFactory, Class<T> clazz, Supplier<T> supplier) {
            Object cachedValue =
                    cache.computeIfAbsent(beanFactory, k -> new HashMap<>()).get(clazz);
            if (cachedValue != null) {
                return cachedValue != NULL ? clazz.cast(cachedValue) : null;
            }
            T value = supplier.get();
            Object cacheValue = value != null ? value : NULL;
            cache.computeIfAbsent(beanFactory, k -> new HashMap<>()).put(clazz, cacheValue);
            return value;
        }
    }
}
