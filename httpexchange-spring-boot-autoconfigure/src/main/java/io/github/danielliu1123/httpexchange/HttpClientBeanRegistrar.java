package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class HttpClientBeanRegistrar {
    private static final Logger log = LoggerFactory.getLogger(HttpClientBeanRegistrar.class);

    private static final Set<BeanDefinitionRegistry> registries = ConcurrentHashMap.newKeySet();
    private static final boolean SPRING_CLOUD_CONTEXT_PRESENT =
            ClassUtils.isPresent("org.springframework.cloud.context.scope.refresh.RefreshScope", null);

    private final ClassPathScanningCandidateComponentProvider scanner;
    private final HttpExchangeProperties properties;
    private final BeanDefinitionRegistry registry;

    public HttpClientBeanRegistrar(HttpExchangeProperties properties, BeanDefinitionRegistry registry) {
        this.scanner = getScanner();
        this.properties = properties;
        this.registry = registry;
        registries.add(registry);
    }

    /**
     * @return whether this {@link BeanDefinitionRegistry} has been registered
     */
    public static boolean hasRegistered(BeanDefinitionRegistry registry) {
        return registries.contains(registry);
    }

    /**
     * Register HTTP client beans for base packages, using {@link HttpExchangeProperties#getBasePackages()} if not specify base packages.
     *
     * @param basePackages base packages to scan
     */
    public void register(String... basePackages) {
        Set<String> packages = Set.copyOf(Arrays.asList(basePackages));
        registerBeans4BasePackages(packages);
    }

    public void register(Class<?>... clients) {
        for (Class<?> client : clients) {
            registerHttpClientBean(registry, client.getName());
        }
    }

    /**
     * Register HTTP client beans the specified class name.
     *
     * @param registry  {@link BeanDefinitionRegistry}
     * @param className class name of HTTP client interface
     */
    @SneakyThrows
    private void registerHttpClientBean(BeanDefinitionRegistry registry, String className) {
        Class<?> clz = Class.forName(className);

        if (!clz.isInterface()) {
            throw new IllegalArgumentException(className + " is not an interface");
        }

        boolean hasHttpExchangeAnnotation = hasAnnotation(clz, HttpExchange.class);

        if (!hasHttpExchangeAnnotation && !hasAnnotation(clz, RequestMapping.class)) {
            return;
        }

        Assert.isInstanceOf(ConfigurableBeanFactory.class, registry);

        ExchangeClientCreator creator =
                new ExchangeClientCreator((ConfigurableBeanFactory) registry, clz, hasHttpExchangeAnnotation);

        AbstractBeanDefinition abd = BeanDefinitionBuilder.genericBeanDefinition(clz, creator::create)
                .getBeanDefinition();

        abd.setPrimary(true);
        abd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        abd.setLazyInit(true);

        try {
            if (properties.getRefresh().isEnabled() && SPRING_CLOUD_CONTEXT_PRESENT) {
                abd.setScope("refresh");
                BeanDefinitionHolder scopedProxy =
                        ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(abd, className), registry, false);
                BeanDefinitionReaderUtils.registerBeanDefinition(scopedProxy, registry);
            } else {
                BeanDefinitionReaderUtils.registerBeanDefinition(new BeanDefinitionHolder(abd, className), registry);
            }
        } catch (BeanDefinitionOverrideException ignore) {
            // clients are included in base packages
            log.warn(
                    "Remove @HttpExchanges client '{}' from 'clients' property; it's already in base packages",
                    className);
        }
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(@Nonnull AnnotatedBeanDefinition abd) {
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

    private static boolean hasAnnotation(Class<?> clz, Class<? extends Annotation> annotationType) {
        if (AnnotationUtils.findAnnotation(clz, annotationType) != null) {
            return true;
        }
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clz);
        for (Method method : methods) {
            if (AnnotationUtils.findAnnotation(method, annotationType) != null) {
                return true;
            }
        }
        return false;
    }

    private void registerBeans4BasePackages(Collection<String> basePackages) {
        for (String pkg : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(pkg);
            for (BeanDefinition bd : beanDefinitions) {
                if (bd.getBeanClassName() != null) {
                    registerHttpClientBean(registry, bd.getBeanClassName());
                }
            }
        }
    }

    static void clear() {
        registries.clear();
    }
}
