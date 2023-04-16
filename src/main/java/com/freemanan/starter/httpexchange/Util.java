package com.freemanan.starter.httpexchange;

import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    public static Optional<HttpClientsProperties.Client> findMatchedConfig(
            Class<?> clz, HttpClientsProperties properties) {
        // find from all clientClass first
        Optional<HttpClientsProperties.Client> found = properties.getClients().stream()
                .filter(it -> it.getClientClass() == clz)
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from the normal way
        return properties.getClients().stream().filter(it -> match(clz, it)).findFirst();
    }

    public static Optional<Class<?>> findMatchedClientClass(
            HttpClientsProperties.Client client, Set<Class<?>> classes) {
        // directly find from the normal way, the found one may not correct
        return classes.stream().filter(clz -> match(clz, client)).findFirst();
    }

    static boolean match(Class<?> clz, HttpClientsProperties.Client client) {
        if (clz == client.getClientClass()) {
            return true;
        }
        String name = client.getName().replaceAll("-", "");
        return name.equalsIgnoreCase(clz.getSimpleName())
                || name.equalsIgnoreCase(clz.getName())
                || name.equalsIgnoreCase(clz.getCanonicalName());
    }
}
