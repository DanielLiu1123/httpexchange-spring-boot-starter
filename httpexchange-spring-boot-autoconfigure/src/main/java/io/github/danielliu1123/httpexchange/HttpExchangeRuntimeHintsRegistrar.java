package io.github.danielliu1123.httpexchange;

import static org.springframework.aot.hint.MemberCategory.ACCESS_DECLARED_FIELDS;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 * @since 3.2.2
 */
class HttpExchangeRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflection = hints.reflection();

        reflection.registerType(HttpServiceProxyFactory.Builder.class, ACCESS_DECLARED_FIELDS);
    }
}
