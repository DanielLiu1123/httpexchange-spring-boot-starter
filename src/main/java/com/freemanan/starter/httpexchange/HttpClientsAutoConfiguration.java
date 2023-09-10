package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.httpexchange.Util.nameMatch;

import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Http Clients Auto Configuration.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = HttpClientsProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(HttpClientsProperties.class)
public class HttpClientsAutoConfiguration implements SmartInitializingSingleton, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(HttpClientsAutoConfiguration.class);

    private final HttpClientsProperties properties;

    public HttpClientsAutoConfiguration(HttpClientsProperties properties) {
        this.properties = properties;
    }

    @Bean
    static HttpClientBeanDefinitionRegistry httpClientBeanDefinitionRegistry() {
        return new HttpClientBeanDefinitionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanParamArgumentResolver beanParamArgumentResolver(HttpClientsProperties properties) {
        return new BeanParamArgumentResolver(properties);
    }

    @Override
    public void afterSingletonsInstantiated() {
        warningUnusedConfiguration();
    }

    @Override
    public void destroy() {
        Cache.clear();
        HttpClientBeanRegistrar.clear();
    }

    private void warningUnusedConfiguration() {
        // Identify the configuration items that are not taking effect and print warning messages.
        Set<Class<?>> classes = Cache.getClients().keySet();

        List<HttpClientsProperties.Channel> channels = properties.getChannels();

        for (int i = 0; i < channels.size(); i++) {
            HttpClientsProperties.Channel channel = channels.get(i);

            checkClassesConfiguration(classes, i, channel);

            checkClientsConfiguration(classes, i, channel);
        }
    }

    private static void checkClassesConfiguration(Set<Class<?>> classes, int i, HttpClientsProperties.Channel channel) {
        int s = channel.getClasses().size();
        for (int j = 0; j < s; j++) {
            Class<?> clazz = channel.getClasses().get(j);
            if (!classes.contains(clazz)) {
                log.warn(
                        "The configuration item '{}.channels[{}].classes[{}]={}' doesn't take effect, please remove it!",
                        HttpClientsProperties.PREFIX,
                        i,
                        j,
                        clazz.getCanonicalName());
            }
        }
    }

    private static void checkClientsConfiguration(Set<Class<?>> classes, int i, HttpClientsProperties.Channel channel) {
        int size = channel.getClients().size();
        for (int j = 0; j < size; j++) {
            String name = channel.getClients().get(j);
            if (!nameMatch(name, classes)) {
                log.warn(
                        "The configuration item '{}.channels[{}].clients[{}]={}' doesn't take effect, please remove it!",
                        HttpClientsProperties.PREFIX,
                        i,
                        j,
                        name);
            }
        }
    }
}
