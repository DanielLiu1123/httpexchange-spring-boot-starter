package io.github.danielliu1123.httpexchange.factory.jdkclient;

import static io.github.danielliu1123.httpexchange.HttpExchangeClientHttpRequestInterceptor.REQUEST_TIMEOUT_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
class ShadedJdkClientHttpRequest extends ShadedAbstractStreamingClientHttpRequest {

    private static final ShadedOutputStreamPublisher.ByteMapper<ByteBuffer> BYTE_MAPPER = new ByteBufferMapper();

    private static final Set<String> DISALLOWED_HEADERS = disallowedHeaders();

    private final HttpClient httpClient;

    private final HttpMethod method;

    private final URI uri;

    private final Executor executor;

    @Nullable
    private final Duration timeout;

    public ShadedJdkClientHttpRequest(
            HttpClient httpClient, URI uri, HttpMethod method, Executor executor, @Nullable Duration readTimeout) {

        this.httpClient = httpClient;
        this.uri = uri;
        this.method = method;
        this.executor = executor;
        this.timeout = readTimeout;
    }

    @Override
    public HttpMethod getMethod() {
        return this.method;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(
                    buildRequest(headers, body), (name, value) -> !name.equalsIgnoreCase(REQUEST_TIMEOUT_HEADER));
            Optional.ofNullable(headers.getFirst(REQUEST_TIMEOUT_HEADER))
                    .map(Integer::valueOf)
                    .map(Duration::ofMillis)
                    .ifPresent(builder::timeout);
            HttpRequest request = builder.build();

            HttpResponse<InputStream> response =
                    this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            return new ShadedJdkClientHttpResponse(response);
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Could not send request: " + ex.getMessage(), ex);
        }
    }

    private HttpRequest buildRequest(HttpHeaders headers, @Nullable Body body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(this.uri);
        if (this.timeout != null) {
            builder.timeout(this.timeout);
        }

        headers.forEach((headerName, headerValues) -> {
            if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
                for (String headerValue : headerValues) {
                    builder.header(headerName, headerValue);
                }
            }
        });

        builder.method(this.method.name(), bodyPublisher(headers, body));
        return builder.build();
    }

    private HttpRequest.BodyPublisher bodyPublisher(HttpHeaders headers, @Nullable Body body) {
        if (body != null) {
            Flow.Publisher<ByteBuffer> outputStreamPublisher = ShadedOutputStreamPublisher.create(
                    outputStream -> body.writeTo(StreamUtils.nonClosing(outputStream)), BYTE_MAPPER, this.executor);

            long contentLength = headers.getContentLength();
            if (contentLength > 0) {
                return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher, contentLength);
            } else if (contentLength == 0) {
                return HttpRequest.BodyPublishers.noBody();
            } else {
                return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher);
            }
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    /**
     * By default, {@link HttpRequest} does not allow {@code Connection},
     * {@code Content-Length}, {@code Expect}, {@code Host}, or {@code Upgrade}
     * headers to be set, but this can be overriden with the
     * {@code jdk.httpclient.allowRestrictedHeaders} system property.
     *
     * @see jdk.internal.net.http.common.Utils#getDisallowedHeaders()
     */
    private static Set<String> disallowedHeaders() {
        TreeSet<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(Set.of("connection", "content-length", "expect", "host", "upgrade"));

        String headersToAllow = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
        if (headersToAllow != null) {
            Set<String> toAllow = StringUtils.commaDelimitedListToSet(headersToAllow);
            headers.removeAll(toAllow);
        }
        return Collections.unmodifiableSet(headers);
    }

    private static final class ByteBufferMapper implements ShadedOutputStreamPublisher.ByteMapper<ByteBuffer> {

        @Override
        public ByteBuffer map(int b) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1);
            byteBuffer.put((byte) b);
            byteBuffer.flip();
            return byteBuffer;
        }

        @Override
        public ByteBuffer map(byte[] b, int off, int len) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(len);
            byteBuffer.put(b, off, len);
            byteBuffer.flip();
            return byteBuffer;
        }
    }
}
