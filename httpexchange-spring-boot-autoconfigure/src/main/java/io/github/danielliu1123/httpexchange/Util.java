package io.github.danielliu1123.httpexchange;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
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

    public static Optional<HttpExchangeProperties.Channel> findMatchedConfig(
            Class<?> clz, HttpExchangeProperties properties) {
        // find from classes first
        Optional<HttpExchangeProperties.Channel> found = properties.getChannels().stream()
                .filter(it -> it.getClasses().stream().anyMatch(ch -> ch == clz))
                .findFirst();
        if (found.isPresent()) {
            return found;
        }
        // not class match, try to find from the 'clients' configuration
        return properties.getChannels().stream().filter(it -> match(clz, it)).findFirst();
    }

    public static boolean nameMatch(String name, Set<Class<?>> classes) {
        return classes.stream().anyMatch(clz -> match(name, clz));
    }

    private static boolean match(Class<?> clz, HttpExchangeProperties.Channel client) {
        if (client.getClasses().stream().anyMatch(ch -> ch == clz)) {
            return true;
        }
        return client.getClients().stream().anyMatch(name -> match(name, clz));
    }

    private static boolean match(String name, Class<?> clz) {
        return isMatched(name, clz) || Stream.of(clz.getInterfaces()).anyMatch(it -> isMatched(name, it));
    }

    private static boolean isMatched(String name, Class<?> clz) {
        String nameToUse = name.replace("-", "");
        return nameToUse.equalsIgnoreCase(clz.getSimpleName())
                || nameToUse.equalsIgnoreCase(clz.getName())
                || nameToUse.equalsIgnoreCase(clz.getCanonicalName())
                || matcher.match(name, clz.getCanonicalName())
                || matcher.match(name, clz.getSimpleName());
    }

    public static HttpExchangeProperties getProperties(Environment environment) {
        HttpExchangeProperties properties = Binder.get(environment)
                .bind(HttpExchangeProperties.PREFIX, HttpExchangeProperties.class)
                .orElseGet(HttpExchangeProperties::new);
        properties.afterPropertiesSet();
        return properties;
    }
}
