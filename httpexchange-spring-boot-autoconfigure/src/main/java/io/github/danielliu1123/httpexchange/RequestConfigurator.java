package io.github.danielliu1123.httpexchange;

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
 *     @GetExchange
 *     List<User> list();
 * }
 *
 * UserApi userApi = ...
 * userApi
 *     .withTimeout(10000)
 *     .addHeader("X-Foo", "bar")
 *     .list();
 * }</pre>
 *
 * <p> Why give a default implementation for all methods?
 * <p> Api interface may extend this interface, and the server side may implement api interface; we don't want to the server side to implement {@link RequestConfigurator}.
 * <p> Use dynamic proxy at runtime to create a proxy client that implements {@link RequestConfigurator}.
 *
 * <p> {@link RequestConfigurator} is suitable for client-side use but not for defining a neutral API.
 * Therefore, {@link Requester} is provided for a programmatic way to dynamically set the read timeout.
 *
 * @author Freeman
 * @see RequestConfiguratorBeanPostProcessor
 * @see Requester
 * @since 3.2.1
 * @deprecated from 3.4.0, too hacky, this will be removed in 3.5.0.
 */
@SuppressWarnings("unchecked")
@Deprecated(since = "3.4.0", forRemoval = true)
public interface RequestConfigurator<T extends RequestConfigurator<T>> {

    /**
     * Create a new instance of {@link RequestConfigurator} with read timeout.
     *
     * @param readTimeout read timeout in milliseconds
     * @return a new instance of {@link RequestConfigurator} with read timeout
     */
    default T withTimeout(int readTimeout) {
        return (T) this;
    }

    /**
     * Create a new instance of {@link RequestConfigurator} with header.
     *
     * <p> If the header already exists, existing values will be overwritten.
     *
     * @param header header name
     * @param values header values, if empty, the header will not be added
     * @return a new instance of {@link RequestConfigurator} with header
     */
    default T addHeader(String header, String... values) {
        return (T) this;
    }
}
