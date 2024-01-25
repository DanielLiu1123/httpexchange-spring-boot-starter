package io.github.danielliu1123.httpexchange;

import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Callback interface that can be used to customize a {@link HttpServiceProxyFactory.Builder}.
 *
 * @author Freeman
 * @since 3.2.2
 */
@FunctionalInterface
public interface HttpServiceProxyFactoryCustomizer {

    /**
     * Customize the {@link HttpServiceProxyFactory.Builder}.
     *
     * @param builder the builder to customize
     */
    void customize(HttpServiceProxyFactory.Builder builder);
}
