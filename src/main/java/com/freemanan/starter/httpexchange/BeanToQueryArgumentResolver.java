package com.freemanan.starter.httpexchange;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

/**
 * {@link BeanToQueryArgumentResolver} used to convert Java bean to request parameters.
 *
 * <p> In Spring web, GET request query parameters will be filled into Java bean by default.
 *
 * <p> {@code Spring Cloud OpenFeign} or {@code Http Exchange} of Spring 6 does not support this feature by default.
 *
 * <p> NOTE: make this class as public, give a chance to be replaced by user.
 *
 * @author Freeman
 * @see HttpClientsConfiguration#beanToQueryArgumentResolver
 */
public class BeanToQueryArgumentResolver implements HttpServiceArgumentResolver, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BeanToQueryArgumentResolver.class);

    public static final int ORDER = 0;

    private static final String webBindAnnotationPackage = RequestParam.class.getPackageName();

    @Override
    public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
        if (hasWebBindAnnotation(parameter) || argument == null || BeanUtils.isSimpleValueType(argument.getClass())) {
            // if there is @RequestParam, @PathVariable, @RequestHeader, @CookieValue, etc,
            // we can not convert Java bean to request parameters,
            // it will be resolved by other ArgumentResolver.
            return false;
        }

        Method method = parameter.getMethod();
        if (method == null || AnnotationUtils.findAnnotation(method, HttpExchange.class) == null) {
            return false;
        }

        if (argument instanceof Map) {
            /*
            NOTE: why not convert map to request parameters?

            The following code will not fill the map with request parameters by default, you need @RequestParam to do that.

            @GetMapping
            public List<Foo> findAll(Map<String, Object> map) {
                return List.of();
            }

            So on the client side, we do the same thing as the server side, DO NOT convert map to request parameters.
            */
            return false;
        }

        Map<String, Object> nameToValue = getPropertyValueMap(argument);
        if (CollectionUtils.isEmpty(nameToValue)) {
            // means Java bean has no property,
            // don't plan to support this case, just mark it as not resolved.
            log.warn(
                    "class '{}' has no property, will not try to convert it to request parameters",
                    argument.getClass().getSimpleName());
            return false;
        }

        nameToValue.forEach((k, v) -> {
            if (v != null && BeanUtils.isSimpleValueType(v.getClass())) {
                requestValues.addRequestParameter(k, v.toString());
            }
        });

        return true;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /**
     * Get property name to value map.
     *
     * @param source Java bean
     * @return property name to value map
     */
    protected static Map<String, Object> getPropertyValueMap(Object source) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            BeanWrapper src = new BeanWrapperImpl(source);
            PropertyDescriptor[] pds = src.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                String name = pd.getName();
                Object srcValue = src.getPropertyValue(name);
                if (!"class".equals(name)) {
                    result.put(name, srcValue);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to convert object[{}] to request parameters", source.getClass(), e);
        }
        return result;
    }

    protected static boolean hasWebBindAnnotation(MethodParameter parameter) {
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            if (annotation.annotationType().getPackageName().startsWith(webBindAnnotationPackage)) {
                return true;
            }
        }
        return false;
    }
}
