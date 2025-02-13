package io.github.danielliu1123.httpexchange;

import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
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

        // See ConfigurerCopier
        reflection.registerType(RestClientBuilderConfigurer.class, MemberCategory.values());
        reflection.registerType(RestTemplateBuilderConfigurer.class, MemberCategory.values());

        // I don't know this is necessary; maybe Spring uses it for reflection?
        reflection.registerType(ClientHttpRequestFactorySettings.class, MemberCategory.values());
    }

    private void registerForClientHttpRequestFactories(ReflectionHints reflection) {
        Set<Class<?>> factoryClasses = new HashSet<>(listFactory());
        factoryClasses.forEach(type ->
                reflection.registerType(type, INVOKE_DECLARED_METHODS, DECLARED_FIELDS, INVOKE_DECLARED_CONSTRUCTORS));
    }

    Set<Class<?>> listFactory() {
        return scanner
                .findCandidateComponents(
                        ClientHttpRequestFactory.class.getPackage().getName())
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(StringUtils::hasText)
                .map(HttpExchangeRuntimeHintsRegistrar::forName)
                .filter(Objects::nonNull)
                .filter(ClientHttpRequestFactory.class::isAssignableFrom)
                .collect(Collectors.toSet());
    }

    private static ClassPathScanningCandidateComponentProvider getScanner() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true; // For including abstract classes
            }
        };
        provider.addIncludeFilter((metadataReader, metadataReaderFactory) ->
                Arrays.stream(metadataReader.getClassMetadata().getInterfaceNames())
                                .anyMatch(intf -> Objects.equals(intf, ClientHttpRequestFactory.class.getName()))
                        || Stream.ofNullable(metadataReader.getClassMetadata().getSuperClassName())
                                .anyMatch(superClass -> Objects.equals(
                                        superClass, AbstractClientHttpRequestFactoryWrapper.class.getName())));
        return provider;
    }

    private static Class<?> forName(String className) {
        try {
            return ClassUtils.forName(className, null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
