package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Freeman
 * @see org.springframework.web.service.annotation.HttpExchangeBeanRegistrationAotProcessor
 * @since 3.2.2
 */
class HttpExchangeBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

    private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

    @Nullable
    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        ResolvableType beanType = registeredBean.getBeanType();
        Class<?> rawClass = beanType.getRawClass();
        if (rawClass == null || !HttpExchangeFactoryBean.class.isAssignableFrom(rawClass)) {
            return null;
        }
        Class<?> clientType = beanType.getGeneric(0).resolve();
        if (clientType == null) {
            return null;
        }
        return (generationContext, beanRegistrationCode) -> {
            RuntimeHints runtimeHints = generationContext.getRuntimeHints();
            runtimeHints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(clientType));
            for (Method method : clientType.getDeclaredMethods()) {
                registerMethodHints(runtimeHints.reflection(), method);
            }
        };
    }

    private void registerMethodHints(ReflectionHints hints, Method method) {
        hints.registerMethod(method, ExecutableMode.INVOKE);
        for (Parameter parameter : method.getParameters()) {
            registerParameterTypeHints(hints, MethodParameter.forParameter(parameter));
        }
        registerReturnTypeHints(hints, MethodParameter.forExecutable(method, -1));
    }

    private void registerParameterTypeHints(ReflectionHints hints, MethodParameter methodParameter) {
        if (methodParameter.hasParameterAnnotation(RequestBody.class)) {
            this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
        }
    }

    private void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
        if (!void.class.equals(returnTypeParameter.getParameterType())) {
            this.bindingRegistrar.registerReflectionHints(hints, returnTypeParameter.getGenericParameterType());
        }
    }
}
