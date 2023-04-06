package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.httpexchange.Util.findMatchedClientClass;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpClientsProperties.class)
@RequiredArgsConstructor
class HttpClientsConfiguration implements DisposableBean, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(HttpClientsConfiguration.class);

    private final HttpClientsProperties properties;

    @Override
    public void afterSingletonsInstantiated() {
        // Identify the configuration items that are not taking effect and print warning messages.
        Set<Class<?>> classes = Cache.getClientClasses();
        properties.getClients().stream()
                .filter(it -> findMatchedClientClass(it.getName(), classes).isEmpty())
                .forEach(it -> log.warn(
                        "The configuration item '{}' is not taking effect, no matched http client found, please check your configuration.",
                        it.getName()));
    }

    @Override
    public void destroy() {
        Cache.clear();
    }
}
