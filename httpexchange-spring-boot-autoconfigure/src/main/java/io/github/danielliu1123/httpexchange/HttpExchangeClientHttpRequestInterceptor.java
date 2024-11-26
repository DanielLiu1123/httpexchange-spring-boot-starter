package io.github.danielliu1123.httpexchange;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 * @deprecated This class will be removed in the 3.5.0.
 */
@Deprecated(since = "3.4.0", forRemoval = true)
public class HttpExchangeClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    public static final String REQUEST_TIMEOUT_HEADER = "X-HttpExchange-Request-Timeout";

    @Override
    @Nonnull
    public ClientHttpResponse intercept(
            @Nonnull HttpRequest request, @Nonnull byte[] body, @Nonnull ClientHttpRequestExecution execution)
            throws IOException {
        HttpExchangeMetadata metadata = HttpExchangeMetadata.get();
        if (metadata == null) {
            return execution.execute(request, body);
        }

        HttpHeaders headers = request.getHeaders();
        if (metadata.getReadTimeout() != null) {
            headers.add(REQUEST_TIMEOUT_HEADER, metadata.getReadTimeout().toString());
        }
        if (!ObjectUtils.isEmpty(metadata.getHeaders())) {
            headers.putAll(metadata.getHeaders());
        }
        return execution.execute(request, body);
    }
}
