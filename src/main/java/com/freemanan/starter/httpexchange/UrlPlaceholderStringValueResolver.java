package com.freemanan.starter.httpexchange;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.util.StringValueResolver;

/**
 * @author Freeman
 */
public class UrlPlaceholderStringValueResolver implements StringValueResolver {

    private final Environment environment;
    private final ObjectProvider<StringValueResolver> delegateProvider;

    public UrlPlaceholderStringValueResolver(
            Environment environment, ObjectProvider<StringValueResolver> delegateProvider) {
        this.environment = environment;
        this.delegateProvider = delegateProvider;
    }

    @Override
    public String resolveStringValue(String strVal) {
        String resolved = environment.resolveRequiredPlaceholders(strVal);
        StringValueResolver delegate = delegateProvider.getIfAvailable();
        if (delegate != null) {
            return delegate.resolveStringValue(resolved);
        }
        return resolved;
    }
}
