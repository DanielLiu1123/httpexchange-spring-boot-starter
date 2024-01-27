package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpClientBeanRegistrar.isHttpExchangeClient;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.env.Environment;
import org.springframework.javapoet.MethodSpec;

/**
 * @author Freeman
 */
class HttpExchangeBeanFactoryInitializationAotProcessor
        implements BeanRegistrationExcludeFilter, BeanFactoryInitializationAotProcessor {

    @Override
    public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
        // To support AOT (Ahead-Of-Time compilation), we refrain from using FactoryBean.
        // Instead, we utilize BeanFactoryInitializationAotProcessor to manually generate bean definitions.
        // Consequently, must exclude all bean definitions related to the Http exchange interface.

        // About use FactoryBean to support AOT,
        // see https://github.com/spring-projects/spring-framework/issues/30434,
        // https://github.com/DanielThomas/spring-aot-issues/pull/1/files
        return isHttpExchangeClient(registeredBean.getBeanClass());
    }

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            @Nonnull ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            Map<String, BeanDefinition> definitions = listDefinition(beanFactory);
            if (definitions.isEmpty()) {
                return;
            }

            registerProxies(generationContext, definitions);

            MethodReference methodReference = beanFactoryInitializationCode
                    .getMethods()
                    .add("registerHttpExchangeClientBeanDefinitions", method -> buildMethod(method, definitions))
                    .toMethodReference();
            beanFactoryInitializationCode.addInitializer(methodReference);
        };
    }

    private static void registerProxies(GenerationContext generationContext, Map<String, BeanDefinition> definitions) {
        ProxyHints proxies = generationContext.getRuntimeHints().proxies();
        definitions.values().stream()
                .map(beanDefinition -> beanDefinition.getResolvableType().resolve())
                .filter(Objects::nonNull)
                .distinct()
                .map(AopProxyUtils::completeJdkProxyInterfaces)
                .forEach(proxies::registerJdkProxy);
    }

    private static void buildMethod(MethodSpec.Builder method, Map<String, BeanDefinition> definitions) {
        method.addModifiers(Modifier.PUBLIC);
        // See org.springframework.beans.factory.aot.BeanFactoryInitializationCode.addInitializer
        // Support DefaultListableBeanFactory, Environment, and ResourceLoader
        method.addParameter(DefaultListableBeanFactory.class, "beanFactory");
        method.addParameter(Environment.class, "environment");
        definitions.forEach((beanName, beanDefinition) -> {
            Class<?> clientClass = beanDefinition.getResolvableType().resolve();
            method.addStatement(
                    "$T.registerHttpExchangeBean(beanFactory, environment, $T.class)",
                    HttpExchangeUtil.class,
                    clientClass);
        });
    }

    private static Map<String, BeanDefinition> listDefinition(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
            Class<?> clz = beanDefinition.getResolvableType().resolve();
            if (clz != null && isHttpExchangeClient(clz)) {
                beanDefinitions.put(name, beanDefinition);
            }
        }
        return beanDefinitions;
    }
}
