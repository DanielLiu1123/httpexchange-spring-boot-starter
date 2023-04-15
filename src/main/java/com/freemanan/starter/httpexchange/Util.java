package com.freemanan.starter.httpexchange;

import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
@UtilityClass
class Util {

    public static Optional<HttpClientsProperties.Client> findMatchedConfig(
            Class<?> clz, HttpClientsProperties properties) {
        return properties.getClients().stream()
                .filter(it -> StringUtils.hasText(it.getName()))
                .filter(it -> match(clz, it.getName()))
                .findFirst();
    }

    public static Optional<Class<?>> findMatchedClientClass(String name, Set<Class<?>> classes) {
        return classes.stream().filter(clz -> match(clz, name)).findFirst();
    }

    static boolean match(Class<?> clz, String configClientName) {
        String name = configClientName.replaceAll("-", "");
        return name.equalsIgnoreCase(clz.getSimpleName())
                || name.equalsIgnoreCase(clz.getName())
                || name.equalsIgnoreCase(clz.getCanonicalName());
    }
}
