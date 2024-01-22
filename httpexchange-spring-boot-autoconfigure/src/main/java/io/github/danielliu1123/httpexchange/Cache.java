package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.Channel;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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
     * Channel config -> client type -> client.
     */
    private static final Map<Channel, Map<ClientType, Object>> configToHttpClient = new ConcurrentHashMap<>();

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

    @SuppressWarnings("unchecked")
    public static <T> T getConfigToHttpClient(Channel channel, ClientType clientType, Supplier<T> supplier) {
        return (T) configToHttpClient
                .computeIfAbsent(channel, k -> new EnumMap<>(ClientType.class))
                .computeIfAbsent(clientType, k -> supplier.get());
    }

    /**
     * Clear cache.
     */
    public static void clear() {
        classToInstance.clear();
        configToHttpClient.clear();
    }
}
