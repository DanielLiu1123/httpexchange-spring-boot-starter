package com.freemanan.starter.httpexchange;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

/**
 * {@link ObjectToParametersArgumentResolver} used to convert Java bean to request parameters.
 *
 * <p> In Spring web, GET request parameters will be filled into Java bean by default.
 *
 * <p> {@code Spring Cloud OpenFeign} or {@code Http Exchange} of Spring 6 does not support this feature by default.
 *
 * @author Freeman
 */
public class ObjectToParametersArgumentResolver implements HttpServiceArgumentResolver {
    private static final Logger log = LoggerFactory.getLogger(ObjectToParametersArgumentResolver.class);

    @Override
    public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
        if (argument == null) {
            return false;
        }

        Method method = parameter.getMethod();
        if (method == null) {
            return false;
        }

        HttpExchange anno = AnnotationUtils.findAnnotation(method, HttpExchange.class);
        if (anno == null) {
            return false;
        }

        // TODO(Freeman): support other methods (POST, PUT, DELETE)?
        if (!"GET".equalsIgnoreCase(anno.method())) {
            return false;
        }

        if (BeanUtils.isSimpleValueType(argument.getClass())) {
            return false;
        }

        Map<String, Object> nameToValue = getPropertyValueMap(argument);
        if (nameToValue.isEmpty()) {
            return false;
        }

        nameToValue.forEach((k, v) -> {
            if (v != null && BeanUtils.isSimpleValueType(v.getClass())) {
                requestValues.addRequestParameter(k, v.toString());
            }
        });

        return true;
    }

    static Map<String, Object> getPropertyValueMap(Object source) {
        if (source instanceof Map) {
            /*
            NOTE: why not convert map to request parameters?

            The following code will not fill the map with request parameters by default, you need @RequestParam to do that.

            @GetMapping
            public List<Foo> findAll(Map<String, Object> map) {
                return List.of();
            }

            So on the client side, we do the same thing as the server side, DO NOT convert map to request parameters.
             */
            return Map.of();
        }
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
}
