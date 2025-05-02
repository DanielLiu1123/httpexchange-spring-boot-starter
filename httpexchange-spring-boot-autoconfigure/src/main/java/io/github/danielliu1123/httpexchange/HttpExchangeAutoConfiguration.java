package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Checker.checkUnusedConfig;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * Http Exchange Auto Configuration.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = HttpExchangeProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(HttpExchangeProperties.class)
public class HttpExchangeAutoConfiguration
        implements DisposableBean, InitializingBean, ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void afterPropertiesSet() throws Exception {
        checkVersion();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var bf = event.getApplicationContext().getBeanFactory();
        if (bf instanceof BeanDefinitionRegistry bdr) {
            HttpClientBeanRegistrar.clearBeanDefinitionCache(bdr);
        }
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

    @Bean
    @ConditionalOnProperty(
            prefix = HttpExchangeProperties.PREFIX,
            name = "warn-unused-config-enabled",
            matchIfMissing = true)
    public CommandLineRunner httpExchangeStarterUnusedConfigChecker(HttpExchangeProperties properties) {
        return args -> checkUnusedConfig(properties);
    }

    @Override
    public void destroy() {
        Cache.clear();
        HttpClientBeanDefinitionRegistry.scanInfo.clear();
    }

    // AOT support

    @Bean
    static HttpExchangeBeanFactoryInitializationAotProcessor
            httpExchangeStarterHttpExchangeBeanFactoryInitializationAotProcessor() {
        return new HttpExchangeBeanFactoryInitializationAotProcessor();
    }

    private static void checkVersion() {
        // Spring Boot 3.5.0 introduced extensive internal refactoring. To reduce maintenance costs, backward
        // compatibility has been dropped.
        // If you're using a Spring Boot version < 3.5.0, please stick with version 3.4.x.
        var version = SpringBootVersion.getVersion();
        String requiredVersion = "3.5.0";
        if (version.compareTo(requiredVersion) < 0) {
            throw new SpringBootVersionIncompatibleException(version, requiredVersion);
        }
    }
}
