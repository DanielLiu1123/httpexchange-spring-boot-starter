package io.github.danielliu1123.httpexchange;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Freeman
 */
public enum ClientType {
    /**
     * @see RestClient
     */
    REST_CLIENT,
    /**
     * @see WebClient
     */
    WEB_CLIENT,
    /**
     * @see RestTemplate
     */
    REST_TEMPLATE
}
