package com.freemanan.starter.httpexchange;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class ExchangeClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final Logger log = LoggerFactory.getLogger(ExchangeClientsRegistrar.class);

    private ResourceLoader resourceLoader;

    private Environment environment;

    private HttpClientsProperties properties;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        this.properties = getProperties(environment);
        check(properties);
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
                        metadata.getAnnotationAttributes(EnableExchangeClients.class.getName()))
                .orElse(Map.of());

        // Shouldn't scan base packages when using clients property
        // see https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/issues/1
        Class<?>[] clientClasses = (Class<?>[]) attrs.getOrDefault("clients", new Class<?>[0]);
        String[] basePackages = (String[]) attrs.getOrDefault("value", new String[0]);
        if (clientClasses.length > 0) {
            registerClassesAsHttpExchange(registry, clientClasses);
            if (basePackages.length > 0) {
                // @EnableExchangeClients(basePackages = "com.example.api", clients = {UserHobbyApi.class})
                // should scan basePackages and register specified clients
                registerBeansForBasePackages(registry, scanner, basePackages);
            }
            return;
        }

        if (basePackages.length == 0) {
            // @EnableExchangeClients
            // should scan the package of the annotated class
            basePackages = new String[] {ClassUtils.getPackageName(metadata.getClassName())};
        }

        registerBeansForBasePackages(registry, scanner, basePackages);
    }

    private static void check(HttpClientsProperties properties) {
        // check if there are duplicated client names
        properties.getClients().stream()
                .map(HttpClientsProperties.Client::getName)
                .filter(StringUtils::hasText)
                .collect(groupingBy(Function.identity(), counting()))
                .forEach((name, count) -> {
                    if (count > 1) {
                        log.warn("There are {} clients with name '{}', please check your configuration", count, name);
                    }
                });

        // check if there are duplicated client classes
        properties.getClients().stream()
                .map(HttpClientsProperties.Client::getClientClass)
                .filter(Objects::nonNull)
                .collect(groupingBy(Function.identity(), counting()))
                .forEach((clz, count) -> {
                    if (count > 1) {
                        log.warn("There are {} clients with class '{}', please check your configuration", count, clz);
                    }
                });
    }

    private static HttpClientsProperties getProperties(Environment environment) {
        HttpClientsProperties properties = Binder.get(environment)
                .bind(HttpClientsProperties.PREFIX, HttpClientsProperties.class)
                .orElseGet(HttpClientsProperties::new);
        properties.afterPropertiesSet();
        return properties;
    }

    private void registerBeansForBasePackages(
            BeanDefinitionRegistry registry,
            ClassPathScanningCandidateComponentProvider scanner,
            String[] basePackages) {
        for (String pkg : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition beanDefinition : beanDefinitions) {
                if (beanDefinition instanceof AnnotatedBeanDefinition bd) {
                    registerHttpExchange(registry, bd.getMetadata().getClassName());
                }
            }
        }
    }

    private void registerHttpExchange(BeanDefinitionRegistry registry, String className) {
        Class<?> clz;
        try {
            clz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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
            Cache.addClientClass(clz);
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
