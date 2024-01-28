package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Util.isHttpExchangeInterface;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class HttpClientBeanRegistrar {

    private static final Set<BeanDefinitionRegistry> registries = ConcurrentHashMap.newKeySet();

    private final ClassPathScanningCandidateComponentProvider scanner;
    private final BeanDefinitionRegistry registry;
    private final Environment environment;

    public HttpClientBeanRegistrar(BeanDefinitionRegistry registry, Environment environment) {
        this.scanner = getScanner();
        this.registry = registry;
        this.environment = environment;
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

        if (!isHttpExchangeInterface(clz)) {
            return;
        }

        Assert.isInstanceOf(ConfigurableBeanFactory.class, registry);

        HttpExchangeUtil.registerHttpExchangeBean((DefaultListableBeanFactory) registry, environment, clz);
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

    static void clear() {
        registries.clear();
    }
}
