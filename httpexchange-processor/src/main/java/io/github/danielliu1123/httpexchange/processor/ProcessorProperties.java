package io.github.danielliu1123.httpexchange.processor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Freeman
 */
record ProcessorProperties(
        boolean enabled, String prefix, String suffix, List<String> packages, String outputSubpackage) {

    public static ProcessorProperties of(Properties properties) {
        boolean enabled = Optional.ofNullable(properties.getProperty("enabled"))
                .map(Boolean::parseBoolean)
                .orElse(true);
        String prefix = Optional.ofNullable(properties.getProperty("prefix")).orElse("");
        String suffix = Optional.ofNullable(properties.getProperty("suffix")).orElse("");
        List<String> packages = Optional.ofNullable(properties.getProperty("packages")).stream()
                .map(list -> list.split(","))
                .flatMap(Arrays::stream)
                .map(String::trim)
                .filter(pkg -> !pkg.isBlank())
                .toList();
        String outputSubpackage =
                Optional.ofNullable(properties.getProperty("outputSubpackage")).orElse("");
        return new ProcessorProperties(enabled, prefix, suffix, packages, outputSubpackage);
    }
}
