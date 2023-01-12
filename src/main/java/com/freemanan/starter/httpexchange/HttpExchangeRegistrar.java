package com.freemanan.starter.httpexchange;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class HttpExchangeRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final Logger log = LoggerFactory.getLogger(HttpExchangeRegistrar.class);

    private ResourceLoader resourceLoader;

    private Environment environment;

    private HttpServiceProxyFactory factory;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerHttpExchanges(metadata, registry);
    }

    private void registerHttpExchanges(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();

        Map<String, Object> attrs = Optional.ofNullable(
                        metadata.getAnnotationAttributes(EnableHttpExchanges.class.getName()))
                .orElse(Collections.emptyMap());

        Optional.ofNullable(attrs.get("clients"))
                .map(it -> (Class<?>[]) it)
                .ifPresent(classes -> registerClassesAsHttpExchange(registry, classes));

        String[] packages = (String[]) attrs.get("value");
        if (packages.length == 0) {
            packages = new String[] {ClassUtils.getPackageName(metadata.getClassName())};
        }

        for (String pkg : packages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition beanDefinition : beanDefinitions) {
                if (beanDefinition instanceof AnnotatedBeanDefinition bd) {
                    registerHttpExchange(registry, bd.getMetadata().getClassName());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerHttpExchange(BeanDefinitionRegistry registry, String className) {
        HttpServiceProxyFactory factory = getHttpServiceProxyFactory();
        try {
            Class<?> clz = Class.forName(className);

            if (!clz.isInterface()) {
                throw new IllegalArgumentException(className + " is not an interface");
            }

            if (isPureInterface(clz)) {
                return;
            }

            Object client = factory.createClient(clz);

            AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition((Class<Object>) clz, () -> client)
                    .getBeanDefinition();
            abd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

            try {
                registry.registerBeanDefinition(className, abd);
            } catch (BeanDefinitionOverrideException ignore) {
                // clients are included in base packages
                log.warn(
                        "Your @HttpExchanges client '{}' is included in base packages, you can remove it from 'clients' property.",
                        className);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPureInterface(Class<?> clz) {
        HttpExchange httpExchange = AnnotationUtils.findAnnotation(clz, HttpExchange.class);
        if (httpExchange != null) {
            return false;
        }
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clz);
        for (Method method : methods) {
            if (AnnotationUtils.findAnnotation(method, HttpExchange.class) != null) {
                return false;
            }
        }
        return true;
    }

    private HttpServiceProxyFactory getHttpServiceProxyFactory() {
        return this.factory != null
                ? this.factory
                : (this.factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(buildWebClient()))
                        .embeddedValueResolver(this.environment::resolvePlaceholders) // support url placeholder '${}'
                        .build());
    }

    private static WebClient buildWebClient() {
        return WebClient.create();
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, this.environment) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        boolean isCandidate = false;
                        if (beanDefinition.getMetadata().isIndependent()) {
                            if (!beanDefinition.getMetadata().isAnnotation()) {
                                isCandidate = true;
                            }
                        }
                        return isCandidate;
                    }
                };
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> isHttpExchange(metadataReader));
        return scanner;
    }

    private static boolean isHttpExchange(MetadataReader mr) {
        return mr.getClassMetadata().isInterface();
    }

    private void registerClassesAsHttpExchange(BeanDefinitionRegistry registry, Class<?>[] classes) {
        for (Class<?> clz : classes) {
            registerHttpExchange(registry, clz.getName());
        }
    }
}
