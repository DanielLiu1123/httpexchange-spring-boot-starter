package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Util.nameMatch;

import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Http Clients Auto Configuration.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = HttpExchangeProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(HttpExchangeProperties.class)
public class HttpExchangeAutoConfiguration implements CommandLineRunner, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(HttpExchangeAutoConfiguration.class);

    private final HttpExchangeProperties properties;

    public HttpExchangeAutoConfiguration(HttpExchangeProperties properties) {
        this.properties = properties;
    }

    @Bean
    static HttpClientBeanDefinitionRegistry httpClientBeanDefinitionRegistry() {
        return new HttpClientBeanDefinitionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanParamArgumentResolver beanParamArgumentResolver(HttpExchangeProperties properties) {
        return new BeanParamArgumentResolver(properties);
    }

    @Override
    public void run(String... args) throws Exception {
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

        List<HttpExchangeProperties.Channel> channels = properties.getChannels();

        for (int i = 0; i < channels.size(); i++) {
            HttpExchangeProperties.Channel channel = channels.get(i);

            checkClassesConfiguration(classes, i, channel);

            checkClientsConfiguration(classes, i, channel);
        }
    }

    private static void checkClassesConfiguration(
            Set<Class<?>> classes, int i, HttpExchangeProperties.Channel channel) {
        int s = channel.getClasses().size();
        for (int j = 0; j < s; j++) {
            Class<?> clazz = channel.getClasses().get(j);
            if (classes.stream().noneMatch(clazz::isAssignableFrom)) {
                log.warn(
                        "The configuration item '{}.channels[{}].classes[{}]={}' doesn't take effect, please remove it!",
                        HttpExchangeProperties.PREFIX,
                        i,
                        j,
                        clazz.getCanonicalName());
            }
        }
    }

    private static void checkClientsConfiguration(
            Set<Class<?>> classes, int i, HttpExchangeProperties.Channel channel) {
        int size = channel.getClients().size();
        for (int j = 0; j < size; j++) {
            String name = channel.getClients().get(j);
            if (!nameMatch(name, classes)) {
                log.warn(
                        "The configuration item '{}.channels[{}].clients[{}]={}' doesn't take effect, please remove it!",
                        HttpExchangeProperties.PREFIX,
                        i,
                        j,
                        name);
            }
        }
    }
}
