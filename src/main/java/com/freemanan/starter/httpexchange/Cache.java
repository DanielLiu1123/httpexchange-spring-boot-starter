package com.freemanan.starter.httpexchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class Cache {

    private static final Map<ReusableModel, HttpServiceProxyFactory> factoryCache = new ConcurrentHashMap<>();
    private static final Map<ReusableModel, WebClient> webClientCache = new ConcurrentHashMap<>();

    public static HttpServiceProxyFactory getFactory(ReusableModel model, Supplier<HttpServiceProxyFactory> supplier) {
        return factoryCache.computeIfAbsent(model, it -> supplier.get());
    }

    public static WebClient getWebClient(ReusableModel model, Supplier<WebClient> supplier) {
        return webClientCache.computeIfAbsent(model, it -> supplier.get());
    }

    public static void clear() {
        factoryCache.clear();
        webClientCache.clear();
    }
}
