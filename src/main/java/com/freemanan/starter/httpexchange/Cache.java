package com.freemanan.starter.httpexchange;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class Cache {
    private static final Set<Class<?>> clientClasses = ConcurrentHashMap.newKeySet();

    public static void addClientClass(Class<?> type) {
        clientClasses.add(type);
    }

    public static Set<Class<?>> getClientClasses() {
        return Set.copyOf(clientClasses);
    }
}
