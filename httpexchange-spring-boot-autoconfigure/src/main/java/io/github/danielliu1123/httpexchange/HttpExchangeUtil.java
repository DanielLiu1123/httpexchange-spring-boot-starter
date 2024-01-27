package io.github.danielliu1123.httpexchange;

import static org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_BY_TYPE;

import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.NativeDetector;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@Slf4j
@UtilityClass
public class HttpExchangeUtil {

    private static final boolean SPRING_CLOUD_CONTEXT_PRESENT =
            ClassUtils.isPresent("org.springframework.cloud.context.scope.refresh.RefreshScope", null);
    private static final AtomicReference<HttpExchangeProperties.Refresh> refresh = new AtomicReference<>();

    /**
     * Register a {@link HttpExchange} annotated interface as a Spring bean.
     *
     * <p> NOTE: The second parameter {@code environment} is used to build {@link HttpExchangeProperties} if it can't be found in the bean factory (early stage),
     * do NOT try to omit this parameter, {@link Environment} can't get from {@link DefaultListableBeanFactory} on every early stage (e.g., ApplicationContextInitializer).
     *
     * @param beanFactory {@link DefaultListableBeanFactory}
     * @param environment {@link Environment}
     * @param clz         {@link HttpExchange} annotated interface
     */
    public static void registerHttpExchangeBean(
            DefaultListableBeanFactory beanFactory, Environment environment, Class<?> clz) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(
                        clz, () -> new ExchangeClientCreator(beanFactory, clz).create())
                .getBeanDefinition();

        beanDefinition.setLazyInit(true);
        beanDefinition.setPrimary(true);
        beanDefinition.setAutowireMode(AUTOWIRE_BY_TYPE);

        String className = clz.getName();
        try {
            if (getRefresh(environment).isEnabled()
                    && SPRING_CLOUD_CONTEXT_PRESENT
                    && System.getProperty("spring.aot.processing") == null
                    && !NativeDetector.inNativeImage()) {
                beanDefinition.setScope("refresh");
                BeanDefinitionHolder scopedProxy = ScopedProxyUtils.createScopedProxy(
                        new BeanDefinitionHolder(beanDefinition, className), beanFactory, false);
                BeanDefinitionReaderUtils.registerBeanDefinition(scopedProxy, beanFactory);
            } else {
                BeanDefinitionReaderUtils.registerBeanDefinition(
                        new BeanDefinitionHolder(beanDefinition, className), beanFactory);
            }
        } catch (BeanDefinitionOverrideException ignore) {
            // clients are included in base packages
            log.warn(
                    "Remove @HttpExchanges client '{}' from 'clients' property; it's already in base packages",
                    className);
        }
    }

    private static HttpExchangeProperties.Refresh getRefresh(Environment environment) {
        HttpExchangeProperties.Refresh cached = refresh.get();
        if (cached != null) {
            return cached;
        }
        HttpExchangeProperties.Refresh result = Binder.get(environment)
                .bind(HttpExchangeProperties.Refresh.PREFIX, HttpExchangeProperties.Refresh.class)
                .orElseGet(HttpExchangeProperties.Refresh::new);
        refresh.set(result);
        return result;
    }
}
