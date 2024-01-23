package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.Channel;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.ClientType;

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
     * {@link ClientId} to Http client instance.
     */
    private static final Map<ClientId, Object> clientIdToHttpClient = new ConcurrentHashMap<>();

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
    public static <T> T getHttpClient(ClientId clientId, Supplier<T> supplier) {
        return (T) clientIdToHttpClient.computeIfAbsent(clientId, k -> supplier.get());
    }

    /**
     * Clear cache.
     */
    public static void clear() {
        classToInstance.clear();
        clientIdToHttpClient.clear();
    }

    record ClientId(Channel channel, ClientType clientType) {}
}
