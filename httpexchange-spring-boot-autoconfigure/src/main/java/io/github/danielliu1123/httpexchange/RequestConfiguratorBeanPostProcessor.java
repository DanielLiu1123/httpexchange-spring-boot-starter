package io.github.danielliu1123.httpexchange;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Proxy {@link RequestConfigurator} beans to add request metadata.
 *
 * @author Freeman
 * @since 3.2.1
 */
public class RequestConfiguratorBeanPostProcessor implements BeanPostProcessor {

    private static final Method WITH_TIMEOUT_METHOD = getWithTimeoutMethod();
    private static final Method ADD_HEADER_METHOD = getAddHeaderMethod();

    @Nullable
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RequestConfigurator<?>
                && AopUtils.isJdkDynamicProxy(bean)
                && bean instanceof Advised
                // Controller may implement api interface, api interface may extend RequestConfigurator
                && !AnnotatedElementUtils.hasAnnotation(bean.getClass(), Component.class)) {
            return createProxy(bean, new HttpExchangeMetadata());
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private Object createProxy(Object target, HttpExchangeMetadata metadata) {
        Advised advised = (Advised) target;
        Class<?>[] interfaces = Stream.concat(
                        Arrays.stream(advised.getProxiedInterfaces()), Stream.of(RequestConfigurator.class))
                .distinct()
                .toArray(Class[]::new);
        return Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, (proxy, method, args) -> {
            if (ADD_HEADER_METHOD.equals(method)) {
                HttpExchangeMetadata copy = metadata.copy();
                copy.getHeaders().put((String) args[0], (List<String>) args[1]);
                return createProxy(advised, copy);
            }
            if (WITH_TIMEOUT_METHOD.equals(method)) {
                HttpExchangeMetadata copy = metadata.copy();
                copy.setReadTimeout((Integer) args[0]);
                return createProxy(advised, copy);
            }

            if (isNotHttpRequestMethod(method)) {
                return method.invoke(advised, args);
            }

            HttpExchangeMetadata.set(metadata);
            try {
                return method.invoke(advised, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                HttpExchangeMetadata.remove();
            }
        });
    }

    private static boolean isNotHttpRequestMethod(Method method) {
        return !AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class)
                && !AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    @SneakyThrows
    private static Method getAddHeaderMethod() {
        return RequestConfigurator.class.getMethod("addHeader", String.class, List.class);
    }

    @SneakyThrows
    private static Method getWithTimeoutMethod() {
        return RequestConfigurator.class.getMethod("withTimeout", int.class);
    }
}
