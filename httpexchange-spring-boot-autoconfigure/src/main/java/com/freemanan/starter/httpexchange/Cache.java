package com.freemanan.starter.httpexchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {
    /**
     * Cache all clients.
     */
    private static final Map<Class<?>, Object> classToInstance = new ConcurrentHashMap<>();

    private static final Map<HttpClientsProperties.Channel, HttpServiceProxyFactory.Builder> cfgToBuilder =
            new ConcurrentHashMap<>();

    /**
     * Add a client to cache.
     *
     * @param client client
     */
    public static void addClient(Object client) {
        classToInstance.put(AopProxyUtils.ultimateTargetClass(client), client);
    }

    /**
     * Get clients.
     *
     * @return unmodifiable map
     */
    public static Map<Class<?>, Object> getClients() {
        return Map.copyOf(classToInstance);
    }

    /**
     * Get or supply a {@link HttpServiceProxyFactory.Builder}.
     *
     * @param cfg      channel config
     * @param supplier {@link HttpServiceProxyFactory.Builder} supplier
     * @return {@link HttpServiceProxyFactory.Builder}
     */
    public static HttpServiceProxyFactory.Builder getOrSupply(
            HttpClientsProperties.Channel cfg, Supplier<HttpServiceProxyFactory.Builder> supplier) {
        return cfgToBuilder.computeIfAbsent(cfg, k -> supplier.get());
    }

    /**
     * Clear cache.
     */
    public static void clear() {
        classToInstance.clear();
        cfgToBuilder.clear();
    }
}
