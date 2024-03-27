package io.github.danielliu1123.httpexchange;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Freeman
 * @since 3.2.4
 */
public interface HttpExchangeAdapterCustomizer<T> {

    /**
     * Customize the client builder with the given config.
     *
     * @param clientBuilder the builder to customize
     * @param config        the config to use
     */
    void customize(T clientBuilder, HttpExchangeProperties.Channel config);

    interface RestClientCustomizer extends HttpExchangeAdapterCustomizer<RestClient.Builder> {}

    interface RestTemplateCustomizer extends HttpExchangeAdapterCustomizer<RestTemplate> {}

    interface WebClientCustomizer extends HttpExchangeAdapterCustomizer<WebClient.Builder> {}
}
