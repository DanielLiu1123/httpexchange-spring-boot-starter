package com.freemanan.starter.httpexchange;

import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    private static final AntPathMatcher matcher = new AntPathMatcher(".");

    public static Optional<HttpClientsProperties.Channel> findMatchedConfig(
            Class<?> clz, HttpClientsProperties properties) {
        // find from classes first
        Optional<HttpClientsProperties.Channel> found = properties.getChannels().stream()
                .filter(it -> it.getClasses().stream().anyMatch(ch -> ch == clz))
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from the clients configuration
        return properties.getChannels().stream().filter(it -> match(clz, it)).findFirst();
    }

    public static boolean nameMatch(String name, Set<Class<?>> classes) {
        return classes.stream().anyMatch(clz -> match(name, clz));
    }

    private static boolean match(Class<?> clz, HttpClientsProperties.Channel client) {
        if (client.getClasses().stream().anyMatch(ch -> ch == clz)) {
            return true;
        }
        return client.getClients().stream().anyMatch(name -> match(name, clz));
    }

    private static boolean match(String name, Class<?> clz) {
        String nameToUse = name.replace("-", "");
        return nameToUse.equalsIgnoreCase(clz.getSimpleName())
                || nameToUse.equalsIgnoreCase(clz.getName())
                || nameToUse.equalsIgnoreCase(clz.getCanonicalName())
                || matcher.match(name, clz.getCanonicalName())
                || matcher.match(name, clz.getSimpleName());
    }

    public static HttpClientsProperties getProperties(Environment environment) {
        HttpClientsProperties properties = Binder.get(environment)
                .bind(HttpClientsProperties.PREFIX, HttpClientsProperties.class)
                .orElseGet(HttpClientsProperties::new);
        properties.merge();
        return properties;
    }
}
