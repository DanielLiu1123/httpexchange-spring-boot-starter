package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Util.isHttpExchangeInterface;

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

    private static final HashMap<BeanDefinitionRegistry, Map<Class<?>, List<BeanDefinition>>> beanDefinitionMap =
            new HashMap<>();

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
            registerHttpClientBean(registry, client);
        }
    }

    /**
     * Register HTTP client beans the specified class name.
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @param clz      class name
     */
    @SneakyThrows
    private void registerHttpClientBean(BeanDefinitionRegistry registry, Class<?> clz) {
        if (!clz.isInterface()) {
            throw new IllegalArgumentException(clz.getName() + " is not an interface");
        }

        if (!isHttpExchangeInterface(clz)) {
            return;
        }

        if (!(registry instanceof DefaultListableBeanFactory bf)) {
            throw new IllegalArgumentException("BeanDefinitionRegistry is not a DefaultListableBeanFactory");
        }

        addBeanDefinitionCache(bf);

        if (hasManualRegistered(registry, clz)) {
            if (log.isDebugEnabled()) {
                log.debug("HTTP client bean '{}' is already registered, skip auto registration", clz.getName());
            }
            return;
        }

        HttpExchangeUtil.registerHttpExchangeBean(bf, environment, clz);
    }

    private static void addBeanDefinitionCache(DefaultListableBeanFactory bf) {
        if (beanDefinitionMap.containsKey(bf)) {
            return;
        }
        for (var beanDefinitionName : bf.getBeanDefinitionNames()) {
            var beanDefinition = bf.getBeanDefinition(beanDefinitionName);
            var clz = Util.getBeanDefinitionClass(beanDefinition);
            if (clz != null) {
                beanDefinitionMap
                        .computeIfAbsent(bf, k -> new HashMap<>())
                        .computeIfAbsent(clz, k -> new ArrayList<>())
                        .add(beanDefinition);
            }
        }
    }

    private static boolean hasManualRegistered(BeanDefinitionRegistry registry, Class<?> clz) {
        return !beanDefinitionMap
                .getOrDefault(registry, Map.of())
                .getOrDefault(clz, List.of())
                .isEmpty();
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition abd) {
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
                var clz = Util.getBeanDefinitionClass(bd);
                if (clz != null) {
                    registerHttpClientBean(registry, clz);
                }
            }
        }
    }

    static void clearBeanDefinitionCache(BeanDefinitionRegistry registry) {
        beanDefinitionMap.remove(registry); // Only used in startup phase
    }
}
