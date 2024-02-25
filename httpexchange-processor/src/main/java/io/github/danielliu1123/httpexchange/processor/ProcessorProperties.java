package io.github.danielliu1123.httpexchange.processor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
record ProcessorProperties(
        boolean enabled,
        String prefix,
        String suffix,
        GeneratedType generatedType,
        List<String> packages,
        String outputSubpackage) {

    public static ProcessorProperties from(Properties properties) {
        boolean enabled = Optional.ofNullable(properties.getProperty("enabled"))
                .map(Boolean::parseBoolean)
                .orElse(true);
        String prefix = Optional.ofNullable(properties.getProperty("prefix")).orElse("");
        String suffix = Optional.ofNullable(properties.getProperty("suffix")).orElse("");
        GeneratedType generatedType = Optional.ofNullable(properties.getProperty("generatedType"))
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .map(GeneratedType::valueOf)
                .orElse(GeneratedType.ABSTRACT_CLASS);
        List<String> packages = Optional.ofNullable(properties.getProperty("packages")).stream()
                .map(list -> list.split(","))
                .flatMap(Arrays::stream)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        String outputSubpackage =
                Optional.ofNullable(properties.getProperty("outputSubpackage")).orElse("");
        return new ProcessorProperties(enabled, prefix, suffix, generatedType, packages, outputSubpackage);
    }
}
