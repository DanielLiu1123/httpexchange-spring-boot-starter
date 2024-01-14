package io.github.danielliu1123.httpexchange;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
class HttpExchangeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.reflection()
                .registerType(
                        TypeReference.of(HttpServiceProxyFactory.Builder.class),
                        builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS))
                .registerType(
                        TypeReference.of(RequestConfigurator.class),
                        builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
    }
}
