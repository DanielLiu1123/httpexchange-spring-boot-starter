package io.github.danielliu1123.httpexchange;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Freeman
 */
public enum ExchangeClientBackend {
    /**
     * Use {@link RestClient} as backend.
     */
    REST_CLIENT,
    /**
     * Use {@link WebClient} as backend.
     */
    WEB_CLIENT,
    /**
     * Use {@link RestTemplate} as backend.
     */
    REST_TEMPLATE
}
