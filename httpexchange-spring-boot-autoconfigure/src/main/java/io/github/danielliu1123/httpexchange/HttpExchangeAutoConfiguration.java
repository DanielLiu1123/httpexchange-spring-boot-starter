package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Checker.checkUnusedConfig;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

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
    static RequestConfiguratorBeanPostProcessor requestConfiguratorBeanPostProcessor() {
        return new RequestConfiguratorBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanParamArgumentResolver beanParamArgumentResolver(HttpExchangeProperties properties) {
        return new BeanParamArgumentResolver(properties);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = HttpExchangeProperties.PREFIX,
            name = "warn-unused-config-enabled",
            matchIfMissing = true)
    public CommandLineRunner unusedConfigChecker(HttpExchangeProperties properties) {
        return args -> checkUnusedConfig(properties);
    }

    @Bean // RestClientBuilderConfigurer is not lazy :)
    public RestClientCustomizer httpExchangeClientHttpRequestInterceptorRestClientCustomizer() {
        return builder -> builder.requestInterceptor(HttpExchangeClientHttpRequestInterceptor.INSTANCE);
    }

    @Bean
    @Lazy // RestTemplateBuilderConfigurer is lazy :)
    public RestTemplateCustomizer httpExchangeClientHttpRequestInterceptorRestTemplateCustomizer() {
        return builder -> builder.getInterceptors().add(HttpExchangeClientHttpRequestInterceptor.INSTANCE);
    }

    @Override
    public void destroy() {
        Cache.clear();
        HttpClientBeanRegistrar.clear();
    }
}
