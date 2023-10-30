package io.github.danielliu1123.httpexchange;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
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
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // check if already processed by ExchangeClientsRegistrar
        if (HttpClientBeanRegistrar.hasRegistered(registry)) {
            return;
        }
        String[] packages = getBasePackages(environment);
        if (ObjectUtils.isEmpty(packages)) {
            return;
        }
        init(registry);
        registrar.register(packages);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private void init(BeanDefinitionRegistry registry) {
        this.properties = (properties == null ? Util.getProperties(environment) : properties);
        this.registrar = (registrar == null ? new HttpClientBeanRegistrar(properties, registry) : registrar);
    }

    private static String[] getBasePackages(Environment environment) {
        return Binder.get(environment)
                .bind(HttpExchangeProperties.PREFIX + ".base-packages", String[].class)
                .orElse(new String[0]);
    }
}
