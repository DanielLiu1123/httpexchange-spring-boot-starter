package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Util.isHttpExchangeInterface;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class HttpClientBeanRegistrar {

    private static final Logger log = LoggerFactory.getLogger(HttpClientBeanRegistrar.class);
    private final ClassPathScanningCandidateComponentProvider scanner = getScanner();
    private final BeanDefinitionRegistry registry;
    private final Environment environment;

    private Map<String, List<BeanDefinition>> classNameToBeanDefinitions;

    public HttpClientBeanRegistrar(BeanDefinitionRegistry registry, Environment environment) {
        this.registry = registry;
        this.environment = environment;
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

        if (!isHttpExchangeInterface(clz)) {
            return;
        }

        if (!(registry instanceof DefaultListableBeanFactory bf)) {
            throw new IllegalArgumentException("BeanDefinitionRegistry is not a DefaultListableBeanFactory");
        }

        initClassNameToBeanDefinitions(bf);

        if (hasManualRegistered(className)) {
            log.debug("HTTP client bean '{}' is already registered, skip auto registration", className);
            return;
        }

        HttpExchangeUtil.registerHttpExchangeBean(bf, environment, clz);
    }

    private void initClassNameToBeanDefinitions(DefaultListableBeanFactory bf) {
        if (classNameToBeanDefinitions == null) {
            classNameToBeanDefinitions = new HashMap<>();
            for (var beanDefinitionName : bf.getBeanDefinitionNames()) {
                var beanDefinition = bf.getBeanDefinition(beanDefinitionName);
                var type = beanDefinition.getResolvableType();
                if (!ResolvableType.NONE.equalsType(type)) {
                    Class<?> clz = type.resolve();
                    if (clz != null) {
                        classNameToBeanDefinitions
                                .computeIfAbsent(clz.getName(), k -> new ArrayList<>())
                                .add(beanDefinition);
                    }
                }
            }
        }
    }

    private boolean hasManualRegistered(String className) {
        var bds = classNameToBeanDefinitions.getOrDefault(className, List.of());
        return !bds.isEmpty();
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(@Nonnull AnnotatedBeanDefinition abd) {
                return true;
            }
        };
        provider.addIncludeFilter((mr, mrf) -> isHttpExchange(mr));
        return provider;
    }

    private static boolean isHttpExchange(MetadataReader mr) {
        ClassMetadata cm = mr.getClassMetadata();
        AnnotationMetadata am = mr.getAnnotationMetadata();
        return cm.isInterface()
                && cm.isIndependent()
                && !cm.isAnnotation()
                && (am.hasAnnotatedMethods(HttpExchange.class.getName())
                        || am.hasAnnotatedMethods(RequestMapping.class.getName()));
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
}
