package io.github.danielliu1123.httpexchange.factory.jdkclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

/**
 * @author Freeman
 */
class ShadedJdkClientHttpResponse implements ClientHttpResponse {

    private final HttpResponse<InputStream> response;

    private final HttpHeaders headers;

    private final InputStream body;

    public ShadedJdkClientHttpResponse(HttpResponse<InputStream> response) {
        this.response = response;
        this.headers = adaptHeaders(response);
        InputStream inputStream = response.body();
        this.body = (inputStream != null ? inputStream : InputStream.nullInputStream());
    }

    private static HttpHeaders adaptHeaders(HttpResponse<?> response) {
        Map<String, List<String>> rawHeaders = response.headers().map();
        Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(rawHeaders.size(), Locale.ENGLISH);
        MultiValueMap<String, String> multiValueMap = CollectionUtils.toMultiValueMap(map);
        multiValueMap.putAll(rawHeaders);
        return HttpHeaders.readOnlyHttpHeaders(multiValueMap);
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return HttpStatusCode.valueOf(this.response.statusCode());
    }

    @Override
    public String getStatusText() {
        // HttpResponse does not expose status text
        if (getStatusCode() instanceof HttpStatus status) {
            return status.getReasonPhrase();
        } else {
            return "";
        }
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.body;
    }

    @Override
    public void close() {
        try {
            try {
                StreamUtils.drain(this.body);
            } finally {
                this.body.close();
            }
        } catch (IOException ignored) {
        }
    }
}
