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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class HttpExchangesRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final Logger log = LoggerFactory.getLogger(HttpExchangesRegistrar.class);

    private ResourceLoader resourceLoader;

    private Environment environment;

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

        // Shouldn't scan base packages when using clients property
        // see https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/issues/1
        Class<?>[] clientClasses = (Class<?>[]) attrs.get("clients");
        if (clientClasses != null && clientClasses.length > 0) {
            registerClassesAsHttpExchange(registry, clientClasses);
            return;
        }

        String[] packages = (String[]) attrs.get("value");
        if (packages == null || packages.length == 0) {
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

    private static void registerHttpExchange(BeanDefinitionRegistry registry, String className) {
        Class<?> clz;
        try {
            clz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (!clz.isInterface()) {
            throw new IllegalArgumentException(className + " is not an interface");
        }

        if (isPureInterface(clz)) {
            return;
        }

        assert registry instanceof ConfigurableBeanFactory;
        HttpExchangeFactory httpExchangeFactory = new HttpExchangeFactory((ConfigurableBeanFactory) registry, clz);

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(clz, httpExchangeFactory::create)
                .getBeanDefinition();
        abd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        abd.setLazyInit(true);

        registry.registerBeanDefinition(className, abd);
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

    private static void registerClassesAsHttpExchange(BeanDefinitionRegistry registry, Class<?>[] classes) {
        for (Class<?> clz : classes) {
            registerHttpExchange(registry, clz.getName());
        }
    }
}
