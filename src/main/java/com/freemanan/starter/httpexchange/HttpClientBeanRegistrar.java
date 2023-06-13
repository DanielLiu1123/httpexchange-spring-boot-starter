package com.freemanan.starter.httpexchange;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
public class HttpClientBeanRegistrar {
    private static final Logger log = LoggerFactory.getLogger(HttpClientBeanRegistrar.class);

    private static final Set<BeanDefinitionRegistry> registries = ConcurrentHashMap.newKeySet();

    private final ClassPathScanningCandidateComponentProvider scanner;
    private final HttpClientsProperties properties;
    private final BeanDefinitionRegistry registry;

    public HttpClientBeanRegistrar(HttpClientsProperties properties, BeanDefinitionRegistry registry) {
        this.scanner = getScanner();
        this.properties = properties;
        this.registry = registry;
        registries.add(registry);
    }

    /**
     * Register HTTP client beans for base packages, using {@link HttpClientsProperties#getBasePackages()} if not specify base packages.
     *
     * @param basePackages base packages to scan
     */
    public void register(String... basePackages) {
        List<String> packages =
                ObjectUtils.isEmpty(basePackages) ? properties.getBasePackages() : Arrays.asList(basePackages);
        registerBeans4BasePackages(packages);
    }

    private void registerHttpClientBean(BeanDefinitionRegistry registry, String className) {
        Class<?> clz;
        try {
            clz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        if (!clz.isInterface()) {
            throw new IllegalArgumentException(className + " is not an interface");
        }

        boolean hasClientSideAnnotation = hasClientSideAnnotation(clz);

        if (!hasClientSideAnnotation && !hasServerSideAnnotation(clz)) {
            return;
        }

        assert registry instanceof ConfigurableBeanFactory;
        ExchangeClientCreator creator =
                new ExchangeClientCreator((ConfigurableBeanFactory) registry, properties, clz, hasClientSideAnnotation);

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(clz, creator::create)
                .getBeanDefinition();

        abd.setPrimary(true);
        abd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        abd.setLazyInit(true);

        try {
            registry.registerBeanDefinition(className, abd);
        } catch (BeanDefinitionOverrideException ignore) {
            // clients are included in base packages
            log.warn(
                    "Your @HttpExchanges client '{}' is included in base packages, you can remove it from 'clients' property.",
                    className);
        }
    }

    private static boolean hasServerSideAnnotation(Class<?> clz) {
        if (AnnotationUtils.findAnnotation(clz, RequestMapping.class) != null) {
            return true;
        }
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clz);
        for (Method method : methods) {
            if (AnnotationUtils.findAnnotation(method, RequestMapping.class) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClientSideAnnotation(Class<?> clz) {
        if (AnnotationUtils.findAnnotation(clz, HttpExchange.class) != null) {
            return true;
        }
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clz);
        for (Method method : methods) {
            if (AnnotationUtils.findAnnotation(method, HttpExchange.class) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether this {@link BeanDefinitionRegistry} has been registered
     */
    public static boolean hasRegistered(BeanDefinitionRegistry registry) {
        return registries.contains(registry);
    }

    private void registerBeans4BasePackages(List<String> basePackages) {
        for (String pkg : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition bd : beanDefinitions) {
                if (bd.getBeanClassName() != null) {
                    registerHttpClientBean(registry, bd.getBeanClassName());
                }
            }
        }
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition abd) {
                return true;
            }
        };
        provider.addIncludeFilter((mr, mrf) -> isHttpClientInterface(mr));
        return provider;
    }

    private static boolean isHttpClientInterface(MetadataReader mr) {
        ClassMetadata cm = mr.getClassMetadata();
        AnnotationMetadata am = mr.getAnnotationMetadata();
        return cm.isInterface()
                && cm.isIndependent()
                && !cm.isAnnotation()
                && (am.hasAnnotatedMethods(HttpExchange.class.getName())
                        || am.hasAnnotatedMethods(RequestMapping.class.getName()));
    }
}
