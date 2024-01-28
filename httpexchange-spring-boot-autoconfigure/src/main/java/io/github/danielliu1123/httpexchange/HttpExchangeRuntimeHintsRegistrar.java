package io.github.danielliu1123.httpexchange;

import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;

import io.github.danielliu1123.httpexchange.shaded.requestfactory.EnhancedJdkClientHttpRequestFactory;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 * @since 3.2.2
 */
class HttpExchangeRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    private final ClassPathScanningCandidateComponentProvider scanner = getScanner();

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflection = hints.reflection();

        reflection.registerType(HttpServiceProxyFactory.Builder.class, DECLARED_FIELDS);

        registerForClientHttpRequestFactories(reflection);
    }

    private void registerForClientHttpRequestFactories(ReflectionHints reflection) {
        Set<Class<?>> classes =
                scanner
                        .findCandidateComponents(
                                ClientHttpRequestFactory.class.getPackage().getName())
                        .stream()
                        .map(BeanDefinition::getBeanClassName)
                        .filter(StringUtils::hasText)
                        .map(HttpExchangeRuntimeHintsRegistrar::forName)
                        .filter(Objects::nonNull)
                        .filter(ClientHttpRequestFactory.class::isAssignableFrom)
                        .collect(Collectors.toSet());
        classes.add(EnhancedJdkClientHttpRequestFactory.class);
        classes.forEach(type ->
                reflection.registerType(type, INVOKE_DECLARED_METHODS, DECLARED_FIELDS, INVOKE_DECLARED_CONSTRUCTORS));
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(MetadataReader metadataReader) {
                return true;
            }
        };
    }

    private static Class<?> forName(String className) {
        try {
            return ClassUtils.forName(className, null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
