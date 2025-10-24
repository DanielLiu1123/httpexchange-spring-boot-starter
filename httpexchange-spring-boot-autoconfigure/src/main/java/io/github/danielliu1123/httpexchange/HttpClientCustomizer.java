package io.github.danielliu1123.httpexchange;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link HttpClientCustomizer} customizes the configuration of the http client based on the given {@link HttpExchangeProperties.Channel}.
 *
 * @author Freeman
 * @since 3.2.4
 */
public sealed interface HttpClientCustomizer<T> {

    /**
     * Customize the client builder with the given config.
     *
     * @param client  the http client to customize
     * @param channel the current channel config to use
     */
    void customize(T client, HttpExchangeProperties.Channel channel);

    non-sealed interface RestClientCustomizer extends HttpClientCustomizer<RestClient.Builder> {}

    non-sealed interface WebClientCustomizer extends HttpClientCustomizer<WebClient.Builder> {}
}
