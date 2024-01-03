package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nonnull;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
class HttpClientBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;
    private HttpExchangeProperties properties;
    private HttpClientBeanRegistrar registrar;

    @Override
    public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) throws BeansException {
        // check if already processed by ExchangeClientsRegistrar
        if (HttpClientBeanRegistrar.hasRegistered(registry)) {
            return;
        }

        this.properties = (properties == null ? Util.getProperties(environment) : properties);
        this.registrar = (registrar == null ? new HttpClientBeanRegistrar(properties, registry) : registrar);

        String[] basePackages = properties.getBasePackages().toArray(String[]::new);
        if (!ObjectUtils.isEmpty(basePackages)) {
            registrar.register(basePackages);
        }
        Set<Class<?>> clients = properties.getClients();
        if (!ObjectUtils.isEmpty(clients)) {
            registrar.register(clients.toArray(Class<?>[]::new));
        }
    }

    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

    @Override
    public void setEnvironment(@Nonnull Environment environment) {
        this.environment = environment;
    }
}
