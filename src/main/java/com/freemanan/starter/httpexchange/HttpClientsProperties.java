package com.freemanan.starter.httpexchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(HttpClientsProperties.PREFIX)
public class HttpClientsProperties implements InitializingBean {
    public static final String PREFIX = "http-exchange";

    /**
     * Default base url.
     */
    private String defaultBaseUrl;
    /**
     * Default response timeout, in milliseconds.
     */
    private Long defaultResponseTimeout;
    /**
     * Default headers, will be merged with {@link Client} headers.
     */
    private Map<String, List<String>> defaultHeaders = new HashMap<>();

    private List<Client> clients = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Client {
        /**
         * Client name, must be unique.
         *
         * <p> Can be simple name, or full class name.
         * <p> e.g. {@code FooApi}, {@code com.example.FooApi}, {@code foo-api}
         */
        private String name;
        /**
         * Base url, will be merged with {@link HttpClientsProperties#defaultBaseUrl}.
         */
        private String baseUrl;
        /**
         * Response timeout, in milliseconds, will be merged with {@link HttpClientsProperties#defaultResponseTimeout}.
         */
        private Long responseTimeout;
        /**
         * Headers, will be merged with {@link HttpClientsProperties#defaultHeaders}.
         */
        private Map<String, List<String>> headers = new HashMap<>();
    }

    @Override
    public void afterPropertiesSet() {
        for (Client client : clients) {
            if (client.getBaseUrl() == null) {
                client.setBaseUrl(defaultBaseUrl);
            }
            if (client.getResponseTimeout() == null) {
                client.setResponseTimeout(defaultResponseTimeout);
            }
            // defaultHeaders + client.headers
            Map<String, List<String>> total = new HashMap<>(this.defaultHeaders);
            total.putAll(client.getHeaders());
            client.setHeaders(total);
        }
    }

    HttpClientsProperties.Client defaultClient() {
        return new Client("__default__", defaultBaseUrl, defaultResponseTimeout, defaultHeaders);
    }
}
