package io.github.danielliu1123.httpexchange;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
final class Util {

    private Util() {}

    private static final AntPathMatcher matcher = new AntPathMatcher(".");

    public static List<HttpExchangeProperties.Channel> findMatchedConfigs(
            Class<?> clz, HttpExchangeProperties properties) {
        List<HttpExchangeProperties.Channel> matchedChannels = new ArrayList<>();
        for (var channel : properties.getChannels()) {
            boolean matched = false;
            if (matchAnyClassesConfig(clz, channel)) {
                matchedChannels.add(channel);
                matched = true;
            }
            if (!matched && matchAnyClientsConfig(clz, channel)) {
                matchedChannels.add(channel);
            }
        }
        return matchedChannels;
    }

    public static boolean nameMatch(String name, Set<Class<?>> classes) {
        return classes.stream().anyMatch(clz -> match(name, clz));
    }

    private static boolean matchAnyClientsConfig(Class<?> clz, HttpExchangeProperties.Channel client) {
        return client.getClients().stream().anyMatch(name -> match(name, clz));
    }

    private static boolean matchAnyClassesConfig(Class<?> clz, HttpExchangeProperties.Channel client) {
        return client.getClasses().stream().anyMatch(ch -> ch == clz);
    }

    private static boolean match(String name, Class<?> clz) {
        return isMatched(name, clz) || Stream.of(clz.getInterfaces()).anyMatch(it -> isMatched(name, it));
    }

    private static boolean isMatched(String name, Class<?> clz) {
        String nameToUse = name.replace("-", "");
        return nameToUse.equalsIgnoreCase(clz.getSimpleName())
                || nameToUse.equalsIgnoreCase(clz.getName())
                || nameToUse.equalsIgnoreCase(clz.getCanonicalName())
                || matcher.match(name, clz.getCanonicalName())
                || matcher.match(name, clz.getSimpleName());
    }

    public static HttpExchangeProperties getProperties(Environment environment) {
        HttpExchangeProperties properties = Binder.get(environment)
                .bind(HttpExchangeProperties.PREFIX, HttpExchangeProperties.class)
                .orElseGet(HttpExchangeProperties::new);
        properties.afterPropertiesSet();
        return properties;
    }

    public static boolean isHttpExchangeInterface(Class<?> clz) {
        return clz.isInterface()
                && (hasAnnotation(clz, HttpExchange.class) || hasAnnotation(clz, RequestMapping.class));
    }

    public static boolean hasAnnotation(Class<?> clz, Class<? extends Annotation> annotationType) {
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

    /**
     * Get class of bean definition.
     *
     * @param beanDefinition bean definition
     * @return class of bean definition
     */
    @Nullable
    public static Class<?> getBeanDefinitionClass(BeanDefinition beanDefinition) {
        // try to get class from factory method metadata
        // @Configuration + @Bean
        if (beanDefinition instanceof AnnotatedBeanDefinition abd) {
            var metadata = abd.getFactoryMethodMetadata();
            if (metadata != null) {
                // Maybe there has @Conditional on the method,
                // Class may not present.
                return forName(metadata.getReturnTypeName());
            }
        }
        var rt = beanDefinition.getResolvableType();
        if (ResolvableType.NONE.equalsType(rt)) {
            var beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName == null) {
                return null;
            }
            return forName(beanClassName);
        }
        return rt.resolve();
    }

    @Nullable
    public static Class<?> forName(String beanClassName) {
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
