package io.github.danielliu1123.httpexchange;

import static org.springframework.util.ObjectUtils.isEmpty;

import jakarta.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
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
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof RequestConfigurator<?>
                && AopUtils.isJdkDynamicProxy(bean)
                && bean instanceof Advised
                // Controller may implement api interface, api interface may extend RequestConfigurator
                && !AnnotatedElementUtils.hasAnnotation(bean.getClass(), Component.class)) {
            return createProxy(bean, new HttpExchangeMetadata());
        }
        return bean;
    }

    private static Object createProxy(Object client, HttpExchangeMetadata metadata) {
        Class<?>[] interfaces = Stream.concat(
                        Arrays.stream(((Advised) client).getProxiedInterfaces()), Stream.of(RequestConfigurator.class))
                .distinct()
                .toArray(Class[]::new);
        ProxyFactory proxyFactory = new ProxyFactory(interfaces);
        proxyFactory.addAdvice(new RequestConfiguratorMethodInterceptor(client, metadata));
        return proxyFactory.getProxy();
    }

    @SneakyThrows
    private static Method getAddHeaderMethod() {
        return RequestConfigurator.class.getMethod("addHeader", String.class, String[].class);
    }

    @SneakyThrows
    private static Method getWithTimeoutMethod() {
        return RequestConfigurator.class.getMethod("withTimeout", int.class);
    }

    private record RequestConfiguratorMethodInterceptor(Object client, HttpExchangeMetadata metadata)
            implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            Object[] args = invocation.getArguments();
            if (ADD_HEADER_METHOD.equals(method)) {
                HttpExchangeMetadata copy = metadata.copy();
                String[] values = (String[]) args[1];
                if (!isEmpty(values)) {
                    copy.getHeaders().put((String) args[0], List.of(values));
                }
                return createProxy(client, copy);
            }
            if (WITH_TIMEOUT_METHOD.equals(method)) {
                HttpExchangeMetadata copy = metadata.copy();
                copy.setReadTimeout((Integer) args[0]);
                return createProxy(client, copy);
            }

            ReflectionUtils.makeAccessible(method);

            if (isNotHttpRequestMethod(method)) {
                return method.invoke(client, args);
            }

            HttpExchangeMetadata.set(metadata);
            try {
                return method.invoke(client, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                HttpExchangeMetadata.remove();
            }
        }

        private static boolean isNotHttpRequestMethod(Method method) {
            return !AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class)
                    && !AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
        }
    }
}
