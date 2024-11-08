package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nonnull;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.util.UriBuilderFactory;

/**
 * {@link BeanParamArgumentResolver} used to convert Java bean to request parameters.
 *
 * <p> In Spring web, GET request query parameters will be filled into Java bean by default.
 *
 * <p> {@code Spring Cloud OpenFeign} or {@code Http Exchange} of Spring 6 does not support this feature by default.
 *
 * <p> NOTE: make this class as public, give a chance to be replaced by user.
 *
 * @author Freeman
 * @see HttpExchangeAutoConfiguration#beanParamArgumentResolver(HttpExchangeProperties)
 */
public class BeanParamArgumentResolver implements HttpServiceArgumentResolver, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BeanParamArgumentResolver.class);

    private static final boolean springQueryMapPresent = ClassUtils.isPresent(
            "org.springframework.cloud.openfeign.SpringQueryMap", BeanParamArgumentResolver.class.getClassLoader());

    public static final int ORDER = 0;

    private static final String WEB_BIND_ANNOTATION_PACKAGE = RequestParam.class.getPackageName();

    private final HttpExchangeProperties properties;

    public BeanParamArgumentResolver(HttpExchangeProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean resolve(
            Object argument, @Nonnull MethodParameter parameter, @Nonnull HttpRequestValues.Builder requestValues) {
        if (isNonResolvableArgument(argument, parameter)) {
            return false;
        }

        if (hasAnnotation(parameter, BeanParam.class)
                || (springQueryMapPresent && hasAnnotation(parameter, SpringQueryMap.class))) {
            return process(argument, requestValues);
        }

        return properties.isBeanToQueryEnabled() && process(argument, requestValues);
    }

    private static boolean process(Object argument, HttpRequestValues.Builder requestValues) {
        /*
        NOTE: why not convert map to request parameters?

        The following code will not fill the map with request parameters by default, you need @RequestParam to do that.

        @GetMapping
        public List<Foo> findAll(Map<String, Object> map) {
            return List.of();
        }

        So on the client side, we do the same thing as the server side, DO NOT convert map to request parameters.
        */
        if (argument instanceof Map) {
            return false;
        }
        Map<String, Object> nameToValue = getPropertyValueMap(argument);
        populateRequestValuesFromMap(requestValues, nameToValue);
        return true;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static boolean isNonResolvableArgument(Object argument, MethodParameter parameter) {
        // If there is @RequestParam, @PathVariable, @RequestHeader, @CookieValue, etc.,
        // we cannot convert Java bean to request parameters,
        // it will be resolved by other ArgumentResolver.
        return argument == null
                || argument instanceof URI // UrlArgumentResolver
                || argument instanceof HttpMethod // HttpMethodArgumentResolver
                || argument instanceof UriBuilderFactory // UriBuilderFactoryArgumentResolver
                || argument instanceof MultipartFile // RequestPartArgumentResolver
                || BeanUtils.isSimpleValueType(argument.getClass())
                || hasWebBindPackageAnnotation(parameter);
    }

    private static boolean hasAnnotation(MethodParameter parameter, Class<? extends Annotation> annotationClass) {
        for (Annotation anno : parameter.getParameterAnnotations()) {
            if (anno.annotationType() == annotationClass) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> getPropertyValueMap(Object source) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            BeanWrapper src = new BeanWrapperImpl(source);
            PropertyDescriptor[] pds = src.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                String name = pd.getName();
                Object srcValue = src.getPropertyValue(name);
                if (!Objects.equals(name, "class")) {
                    result.put(name, srcValue);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to convert object[{}] to request parameters", source.getClass(), e);
        }
        return result;
    }

    private static void populateRequestValuesFromMap(
            HttpRequestValues.Builder requestValues, Map<String, Object> nameToValue) {
        nameToValue.forEach((k, v) -> {
            if (v == null) {
                return;
            }
            Class<?> clz = v.getClass();
            if (BeanUtils.isSimpleValueType(clz)) {
                requestValues.addRequestParameter(k, v.toString());
            } else if (clz.isArray() && BeanUtils.isSimpleValueType(clz.getComponentType())) {
                String[] arrValue = Arrays.stream((Object[]) v)
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toArray(String[]::new);
                if (arrValue.length > 0) {
                    requestValues.addRequestParameter(k, arrValue);
                }
            } else if (v instanceof Iterable<?> iter) {
                List<String> values = new ArrayList<>();
                iter.forEach(item -> {
                    if (item != null && BeanUtils.isSimpleValueType(item.getClass())) {
                        values.add(item.toString());
                    }
                });
                if (!values.isEmpty()) {
                    requestValues.addRequestParameter(k, values.toArray(String[]::new));
                }
            }
        });
    }

    private static boolean hasWebBindPackageAnnotation(MethodParameter parameter) {
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            if (annotation.annotationType().getPackageName().startsWith(WEB_BIND_ANNOTATION_PACKAGE)) {
                return true;
            }
        }
        return false;
    }
}
