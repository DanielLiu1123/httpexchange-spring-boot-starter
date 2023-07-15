package com.freemanan.starter.httpexchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;
import org.springframework.aop.framework.AopProxyUtils;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {
    /**
     * Cache all clients.
     */
    private static final Map<Class<?>, Object> classToInstance = new ConcurrentHashMap<>();

    /**
     * Add client to cache.
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
     * Clear cache.
     */
    public static void clear() {
        classToInstance.clear();
    }
}
