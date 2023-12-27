package io.github.danielliu1123.httpexchange;

import java.util.List;

/**
 * {@link RequestConfigurator} is used to set request level configurations, such as timeout, headers, etc.
 *
 * <p> This interface is only used for client side.
 *
 * <p> This interface is not intended to be implemented by user.
 *
 * <p>Example:
 * <pre>{@code
 * @HttpExchange("/users")
 * interface UserApi extends RequestConfigurator<UserApi> {
 *     @HttpExchange
 *     List<User> list();
 * }
 *
 * UserApi userApi = ...
 * userApi
 *     .withTimeout(10000)
 *     .addHeader("X-Foo", List.of("bar"))
 *     .list();
 * }</pre>
 *
 * <p> Why give a default implementation for all methods?
 * <p> Api interface may extend this interface, and the server side may implement api interface; we don't want to the server side to implement {@link RequestConfigurator}.
 *
 * @author Freeman
 * @see RequestConfiguratorBeanPostProcessor
 * @since 3.2.1
 */
public interface RequestConfigurator<T extends RequestConfigurator<T>> {

    /**
     * Create a new instance of {@link RequestConfigurator} with read timeout.
     *
     * @param readTimeout read timeout in milliseconds
     * @return a new instance of {@link RequestConfigurator} with read timeout
     */
    default T withTimeout(int readTimeout) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new instance of {@link RequestConfigurator} with header.
     *
     * <p> If the header already exists, existing values will be overwritten.
     *
     * @param header header name
     * @param values header values
     * @return a new instance of {@link RequestConfigurator} with header
     */
    default T addHeader(String header, List<String> values) {
        throw new UnsupportedOperationException();
    }
}
