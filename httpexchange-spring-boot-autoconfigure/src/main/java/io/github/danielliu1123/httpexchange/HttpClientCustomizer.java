package io.github.danielliu1123.httpexchange;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link HttpClientCustomizer} customizes the configuration of the http client based on the given {@link HttpExchangeProperties.Channel}.
 *
 * @author Freeman
 * @see ExchangeClientCreator#buildRestClient(HttpExchangeProperties.Channel)
 * @see ExchangeClientCreator#buildRestTemplate(HttpExchangeProperties.Channel)
 * @see ExchangeClientCreator#buildWebClient(HttpExchangeProperties.Channel)
 * @since 3.2.4
 */
public sealed interface HttpClientCustomizer<T>
        permits HttpClientCustomizer.RestClientCustomizer,
                HttpClientCustomizer.RestTemplateCustomizer,
                HttpClientCustomizer.WebClientCustomizer {

    /**
     * Customize the client builder with the given config.
     *
     * @param client the http client to customize
     * @param config the current channel config to use
     */
    void customize(T client, HttpExchangeProperties.Channel config);

    non-sealed interface RestClientCustomizer extends HttpClientCustomizer<RestClient.Builder> {}

    /**
     * Use {@link RestTemplate} instead of {@link RestTemplateBuilder} because the latter is immutable.
     */
    non-sealed interface RestTemplateCustomizer extends HttpClientCustomizer<RestTemplate> {}

    non-sealed interface WebClientCustomizer extends HttpClientCustomizer<WebClient.Builder> {}
}
