package io.github.danielliu1123.httpexchange;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
public class HttpExchangeClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    public static final String REQUEST_TIMEOUT_HEADER = "X-HttpExchange-Request-Timeout";

    public static final HttpExchangeClientHttpRequestInterceptor INSTANCE =
            new HttpExchangeClientHttpRequestInterceptor();

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
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
