package com.freemanan.starter.httpexchange;

import com.freemanan.starter.httpexchange.shaded.ShadedHttpServiceProxyFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {

    private static final ConcurrentMap<ReusableModel, HttpServiceProxyFactory> factoryCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ReusableModel, ShadedHttpServiceProxyFactory> shadedFactoryCache =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<ReusableModel, WebClient> webClientCache = new ConcurrentHashMap<>();
    private static final Set<Class<?>> clientClasses = ConcurrentHashMap.newKeySet();

    public static HttpServiceProxyFactory getFactory(ReusableModel model, Supplier<HttpServiceProxyFactory> supplier) {
        return factoryCache.computeIfAbsent(model, it -> supplier.get());
    }

    public static ShadedHttpServiceProxyFactory getShadedFactory(
            ReusableModel model, Supplier<ShadedHttpServiceProxyFactory> supplier) {
        return shadedFactoryCache.computeIfAbsent(model, it -> supplier.get());
    }

    public static WebClient getWebClient(ReusableModel model, Supplier<WebClient> supplier) {
        return webClientCache.computeIfAbsent(model, it -> supplier.get());
    }

    public static void addClientClass(Class<?> type) {
        clientClasses.add(type);
    }

    public static Set<Class<?>> getClientClasses() {
        return Set.copyOf(clientClasses);
    }

    public static void clear() {
        factoryCache.clear();
        shadedFactoryCache.clear();
        webClientCache.clear();
        clientClasses.clear();
    }
}
