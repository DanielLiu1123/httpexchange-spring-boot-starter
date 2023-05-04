package com.freemanan.starter.httpexchange;

import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    public static Optional<HttpClientsProperties.Channel> findMatchedConfig(
            Class<?> clz, HttpClientsProperties properties) {
        // find from classes first
        Optional<HttpClientsProperties.Channel> found = properties.getChannels().stream()
                .filter(it -> it.getClasses().stream().anyMatch(ch -> ch == clz))
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from the normal way
        return properties.getChannels().stream().filter(it -> match(clz, it)).findFirst();
    }

    static boolean match(Class<?> clz, HttpClientsProperties.Channel client) {
        if (client.getClasses().stream().anyMatch(ch -> ch == clz)) {
            return true;
        }
        return client.getClients().stream().anyMatch(name -> {
            name = name.replaceAll("-", "");
            return name.equalsIgnoreCase(clz.getSimpleName())
                    || name.equalsIgnoreCase(clz.getName())
                    || name.equalsIgnoreCase(clz.getCanonicalName());
        });
    }

    static boolean nameMatch(String name, Set<Class<?>> classes) {
        String nameToUse = name.replaceAll("-", "");
        return classes.stream()
                .anyMatch(clz -> nameToUse.equalsIgnoreCase(clz.getSimpleName())
                        || nameToUse.equalsIgnoreCase(clz.getName())
                        || nameToUse.equalsIgnoreCase(clz.getCanonicalName()));
    }
}
