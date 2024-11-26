package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Data;

/**
 * {@link HttpExchangeMetadata} is used to store request metadata, such as timeout, headers, etc.
 *
 * <p> Those values will be set to request at runtime.
 *
 * @author Freeman
 * @see HttpExchangeClientHttpRequestInterceptor
 * @see RequestConfiguratorBeanPostProcessor
 * @since 3.2.1
 * @deprecated This class will be removed in the 3.5.0.
 */
@Data
@Deprecated(since = "3.4.0", forRemoval = true)
public class HttpExchangeMetadata {

    private static final ThreadLocal<HttpExchangeMetadata> HOLDER = new InheritableThreadLocal<>();

    /**
     * Read timeout in milliseconds.
     */
    private Integer readTimeout;
    /**
     * Request headers to be added.
     */
    private LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();

    @Nullable
    public static HttpExchangeMetadata get() {
        return HOLDER.get();
    }

    public static void set(HttpExchangeMetadata metadata) {
        HOLDER.set(metadata);
    }

    public static void remove() {
        HOLDER.remove();
    }

    HttpExchangeMetadata copy() {
        HttpExchangeMetadata metadata = new HttpExchangeMetadata();
        metadata.setReadTimeout(getReadTimeout());
        metadata.setHeaders(new LinkedHashMap<>(getHeaders()));
        return metadata;
    }
}
