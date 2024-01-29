package io.github.danielliu1123.httpexchange.shaded.requestfactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Enhanced {@link JdkClientHttpRequestFactory}, support to set timeout for each request.
 *
 * @author Freeman
 * @since 3.2.1
 */
public class EnhancedJdkClientHttpRequestFactory implements ClientHttpRequestFactory {

    private final HttpClient httpClient;

    private final Executor executor;

    @Nullable
    private Duration readTimeout;

    /**
     * Create a new instance of the {@code JdkClientHttpRequestFactory}
     * with a default {@link HttpClient}.
     */
    public EnhancedJdkClientHttpRequestFactory() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Create a new instance of the {@code JdkClientHttpRequestFactory} based on
     * the given {@link HttpClient}.
     *
     * @param httpClient the client to base on
     */
    public EnhancedJdkClientHttpRequestFactory(HttpClient httpClient) {
        Assert.notNull(httpClient, "HttpClient is required");
        this.httpClient = httpClient;
        this.executor = httpClient.executor().orElseGet(SimpleAsyncTaskExecutor::new);
    }

    /**
     * Create a new instance of the {@code JdkClientHttpRequestFactory} based on
     * the given {@link HttpClient} and {@link Executor}.
     *
     * @param httpClient the client to base on
     * @param executor   the executor to use for blocking write operations
     */
    public EnhancedJdkClientHttpRequestFactory(HttpClient httpClient, Executor executor) {
        Assert.notNull(httpClient, "HttpClient is required");
        Assert.notNull(executor, "Executor must not be null");
        this.httpClient = httpClient;
        this.executor = executor;
    }

    /**
     * Set the underlying {@code HttpClient}'s read timeout (in milliseconds).
     * A timeout value of 0 specifies an infinite timeout.
     * <p>Default is the system's default timeout.
     *
     * @see java.net.http.HttpRequest.Builder#timeout
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = Duration.ofMillis(readTimeout);
    }

    /**
     * Set the underlying {@code HttpClient}'s read timeout as a
     * {@code Duration}.
     * <p>Default is the system's default timeout.
     *
     * @see java.net.http.HttpRequest.Builder#timeout
     */
    public void setReadTimeout(Duration readTimeout) {
        Assert.notNull(readTimeout, "ReadTimeout must not be null");
        this.readTimeout = readTimeout;
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        return new ShadedJdkClientHttpRequest(this.httpClient, uri, httpMethod, this.executor, this.readTimeout);
    }
}
