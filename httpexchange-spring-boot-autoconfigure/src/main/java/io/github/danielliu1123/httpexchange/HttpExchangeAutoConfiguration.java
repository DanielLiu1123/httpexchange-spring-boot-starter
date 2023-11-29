package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Checker.checkUnusedConfig;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Http Exchange Auto Configuration.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = HttpExchangeProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(HttpExchangeProperties.class)
public class HttpExchangeAutoConfiguration implements DisposableBean {

    @Bean
    static HttpClientBeanDefinitionRegistry httpClientBeanDefinitionRegistry() {
        return new HttpClientBeanDefinitionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanParamArgumentResolver beanParamArgumentResolver(HttpExchangeProperties properties) {
        return new BeanParamArgumentResolver(properties);
    }

    /**
     * Using {@link java.net.http.HttpClient} as default.
     */
    @Bean
    @ConditionalOnMissingBean(ClientHttpRequestFactory.class)
    public ClientHttpRequestFactory httpExchangeClientHttpRequestFactory() {
        return ClientHttpRequestFactories.get(
                JdkClientHttpRequestFactory.class, ClientHttpRequestFactorySettings.DEFAULTS);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = HttpExchangeProperties.PREFIX,
            name = "warn-unused-config-enabled",
            matchIfMissing = true)
    public CommandLineRunner unusedConfigChecker(HttpExchangeProperties properties) {
        return args -> checkUnusedConfig(properties);
    }

    @Override
    public void destroy() {
        Cache.clear();
        HttpClientBeanRegistrar.clear();
    }
}
