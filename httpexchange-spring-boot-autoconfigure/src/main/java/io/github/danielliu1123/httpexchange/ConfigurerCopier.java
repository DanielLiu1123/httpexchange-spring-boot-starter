package io.github.danielliu1123.httpexchange;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
final class ConfigurerCopier {

    private static final Map<String, Field> restClientBuilderConfigurerProperties;
    private static final Map<String, Field> restTemplateBuilderConfigurerProperties;

    static {
        restClientBuilderConfigurerProperties = Arrays.stream(RestClientBuilderConfigurer.class.getDeclaredFields())
                .peek(ReflectionUtils::makeAccessible)
                .collect(Collectors.toMap(Field::getName, Function.identity()));
        restTemplateBuilderConfigurerProperties = Arrays.stream(RestTemplateBuilderConfigurer.class.getDeclaredFields())
                .peek(ReflectionUtils::makeAccessible)
                .collect(Collectors.toMap(Field::getName, Function.identity()));
    }

    public static RestClientBuilderConfigurer copyRestClientBuilderConfigurer(RestClientBuilderConfigurer source) {

        var target = new RestClientBuilderConfigurer();

        for (var entry : restClientBuilderConfigurerProperties.entrySet()) {
            var field = entry.getValue();
            var value = ReflectionUtils.getField(field, source);
            if (value != null) {
                ReflectionUtils.setField(field, target, value);
            }
        }

        return target;
    }

    public static void setRestClientBuilderConfigurerProperty(
            RestClientBuilderConfigurer target, String name, Object value) {
        var field = restClientBuilderConfigurerProperties.get(name);
        if (field != null) {
            ReflectionUtils.setField(field, target, value);
        }
    }

    public static RestTemplateBuilderConfigurer copyRestTemplateBuilderConfigurer(
            RestTemplateBuilderConfigurer source) {

        var target = new RestTemplateBuilderConfigurer();

        for (var entry : restTemplateBuilderConfigurerProperties.entrySet()) {
            var field = entry.getValue();
            var value = ReflectionUtils.getField(field, source);
            if (value != null) {
                ReflectionUtils.setField(field, target, value);
            }
        }

        return target;
    }

    public static void setRestTemplateBuilderConfigurerProperty(
            RestTemplateBuilderConfigurer target, String name, Object value) {
        var field = restTemplateBuilderConfigurerProperties.get(name);
        if (field != null) {
            ReflectionUtils.setField(field, target, value);
        }
    }
}
