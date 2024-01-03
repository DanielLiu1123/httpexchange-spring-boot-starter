package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringValueResolver;

/**
 * Support for resolving placeholders in {@link String} values.
 *
 * @author Freeman
 */
public class UrlPlaceholderStringValueResolver implements StringValueResolver {
    private static final Logger log = LoggerFactory.getLogger(UrlPlaceholderStringValueResolver.class);

    private final Environment environment;

    @Nullable
    private final StringValueResolver delegate;

    public UrlPlaceholderStringValueResolver(Environment environment, @Nullable StringValueResolver delegate) {
        this.environment = environment;
        this.delegate = delegate;
    }

    @Override
    public String resolveStringValue(@Nonnull String strVal) {
        String resolved = strVal;
        try {
            resolved = environment.resolvePlaceholders(strVal);
        } catch (Exception e) {
            log.warn("Failed to resolve placeholders in '{}'", strVal, e);
        }
        return delegate != null ? delegate.resolveStringValue(resolved) : resolved;
    }

    /**
     * Create a new {@link UrlPlaceholderStringValueResolver} instance.
     *
     * @param environment the environment
     * @param delegate    {@link StringValueResolver}
     * @return {@link UrlPlaceholderStringValueResolver}
     */
    public static UrlPlaceholderStringValueResolver create(
            Environment environment, @Nullable StringValueResolver delegate) {
        return new UrlPlaceholderStringValueResolver(environment, delegate);
    }
}
