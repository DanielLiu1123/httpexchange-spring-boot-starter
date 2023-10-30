package io.github.danielliu1123.httpexchange;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
class ExchangeClientsRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;
    private HttpExchangeProperties properties;
    private HttpClientBeanRegistrar registrar;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        boolean enabled = environment.getProperty(HttpExchangeProperties.PREFIX + ".enabled", Boolean.class, true);
        if (!enabled) {
            return;
        }

        init(registry);

        Map<String, Object> attrs = Optional.ofNullable(
                        metadata.getAnnotationAttributes(EnableExchangeClients.class.getName()))
                .orElse(Map.of());

        // Shouldn't scan base packages when using clients property
        // see https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/issues/1
        Class<?>[] clientClasses = (Class<?>[]) attrs.getOrDefault("clients", new Class<?>[0]);
        String[] basePackages = getBasePackages(attrs);
        if (clientClasses.length > 0) {
            registerClassesAsHttpExchange(registry, clientClasses);
            if (basePackages.length > 0) {
                // @EnableExchangeClients(basePackages = "com.example.api", clients = {UserHobbyApi.class})
                // should scan basePackages and register specified clients
                registrar.register(basePackages);
            }
            return;
        }

        if (basePackages.length == 0) {
            // @EnableExchangeClients
            // should scan the package of the annotated class
            basePackages = new String[] {ClassUtils.getPackageName(metadata.getClassName())};
        }

        registrar.register(basePackages);
    }

    private String[] getBasePackages(Map<String, Object> attrs) {
        String[] basePackages = (String[]) attrs.getOrDefault("value", new String[0]);
        return !ObjectUtils.isEmpty(basePackages)
                ? basePackages
                : properties.getBasePackages().toArray(new String[0]);
    }

    private void init(BeanDefinitionRegistry registry) {
        this.properties = (properties == null ? Util.getProperties(environment) : properties);
        this.registrar = (registrar == null ? new HttpClientBeanRegistrar(properties, registry) : registrar);
    }

    private void registerClassesAsHttpExchange(BeanDefinitionRegistry registry, Class<?>[] classes) {
        for (Class<?> clz : classes) {
            registrar.registerHttpClientBean(registry, clz.getName());
        }
    }
}
