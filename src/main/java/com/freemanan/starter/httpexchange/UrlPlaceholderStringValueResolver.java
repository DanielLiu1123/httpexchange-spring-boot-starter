package com.freemanan.starter.httpexchange;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.util.StringValueResolver;

/**
 * Support for resolving placeholders in {@link String} values.
 *
 * @author Freeman
 */
class UrlPlaceholderStringValueResolver implements StringValueResolver {
    private static final Logger log = LoggerFactory.getLogger(UrlPlaceholderStringValueResolver.class);

    private final Environment environment;

    @Nullable
    private final StringValueResolver delegate;

    UrlPlaceholderStringValueResolver(Environment environment, ObjectProvider<StringValueResolver> delegateProvider) {
        this.environment = environment;
        this.delegate = delegateProvider.getIfUnique();
    }

    @Override
    public String resolveStringValue(String strVal) {
        String resolved;
        try {
            resolved = environment.resolvePlaceholders(strVal);
        } catch (Exception e) {
            log.warn("Failed to resolve placeholders in '{}'", strVal, e);
            resolved = strVal;
        }
        if (delegate != null) {
            return delegate.resolveStringValue(resolved);
        }
        return resolved;
    }
}
