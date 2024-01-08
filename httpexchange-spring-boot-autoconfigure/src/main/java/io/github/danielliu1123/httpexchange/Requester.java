package io.github.danielliu1123.httpexchange;

import java.util.Arrays;
import java.util.function.Supplier;
import org.springframework.util.ObjectUtils;

/**
 * {@link Requester} is used to configure request metadata, such as timeout, headers, etc.
 *
 * <p> Provide a programmatic way to configure request metadata, achieving the same function as {@link RequestConfigurator}.
 *
 * <p> Examples:
 * <pre>{@code
 * List<User> users = Requester.create()
 *                         .withTimeout(10000)
 *                         .addHeader("X-Foo", "bar")
 *                         .call(() -> userApi.list());
 * }</pre>
 *
 * @author Freeman
 * @see RequestConfigurator
 * @since 3.2.1
 */
public final class Requester {

    private Requester() {}

    private final HttpExchangeMetadata metadata = new HttpExchangeMetadata();

    /**
     * Set read timeout in milliseconds.
     *
     * @param readTimeout read timeout in milliseconds
     * @return this
     */
    public Requester withTimeout(int readTimeout) {
        metadata.setReadTimeout(readTimeout);
        return this;
    }

    /**
     * Add header with specified values.
     *
     * <p> If the header already exists, existing values will be overwritten.
     *
     * @param key header name
     * @param values header values, if empty, the header will not be added
     * @return this
     */
    public Requester addHeader(String key, String... values) {
        if (!ObjectUtils.isEmpty(values)) {
            metadata.getHeaders().put(key, Arrays.asList(values));
        }
        return this;
    }

    /**
     * Call supplier with request metadata.
     *
     * @param supplier supplier
     * @param <T> return type
     * @return return value of supplier
     */
    public <T> T call(Supplier<T> supplier) {
        HttpExchangeMetadata.set(metadata);
        try {
            return supplier.get();
        } finally {
            HttpExchangeMetadata.remove();
        }
    }

    /**
     * Call runnable with request metadata.
     *
     * @param runnable runnable
     */
    public void call(Runnable runnable) {
        HttpExchangeMetadata.set(metadata);
        try {
            runnable.run();
        } finally {
            HttpExchangeMetadata.remove();
        }
    }

    /**
     * Create a new instance of {@link Requester}.
     *
     * @return a new instance of {@link Requester}
     */
    public static Requester create() {
        return new Requester();
    }
}
