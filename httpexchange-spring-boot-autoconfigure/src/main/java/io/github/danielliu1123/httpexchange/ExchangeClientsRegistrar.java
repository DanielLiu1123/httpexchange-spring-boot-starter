package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author Freeman
 */
class ExchangeClientsRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(
            @Nonnull AnnotationMetadata metadata, @Nonnull BeanDefinitionRegistry registry) {
        Map<String, Object> attrs = Optional.ofNullable(
                        metadata.getAnnotationAttributes(EnableExchangeClients.class.getName()))
                .orElse(Map.of());

        // Shouldn't scan basePackages when using 'clients' property
        // see https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/issues/1

        String[] basePackages = (String[]) attrs.getOrDefault("value", new String[0]);
        Class<?>[] clientClasses = (Class<?>[]) attrs.getOrDefault("clients", new Class<?>[0]);

        HttpClientBeanDefinitionRegistry.scanInfo.clients.addAll(List.of(clientClasses));
        HttpClientBeanDefinitionRegistry.scanInfo.basePackages.addAll(List.of(basePackages));

        if (basePackages.length == 0 && clientClasses.length == 0) {
            // @EnableExchangeClients
            // should scan the package of the annotated class
            HttpClientBeanDefinitionRegistry.scanInfo.basePackages.add(
                    ClassUtils.getPackageName(metadata.getClassName()));
        }
    }
}
