package com.freemanan.starter.httpexchange;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private String baseUrl;
    /**
     * Default response timeout, in milliseconds.
     */
    private Long responseTimeout;
    /**
     * Default headers.
     */
    private List<Header> headers = new ArrayList<>();

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
         * Base url, will be merged with {@link HttpClientsProperties#baseUrl}.
         */
        private String baseUrl;
        /**
         * Response timeout, in milliseconds, will be merged with {@link HttpClientsProperties#responseTimeout}.
         */
        private Long responseTimeout;
        /**
         * Headers, will be merged with {@link HttpClientsProperties#headers}.
         */
        private List<Header> headers = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String key;
        private List<String> values = new ArrayList<>();
    }

    @Override
    public void afterPropertiesSet() {
        for (Client client : clients) {
            if (client.getBaseUrl() == null) {
                client.setBaseUrl(baseUrl);
            }
            if (client.getResponseTimeout() == null) {
                client.setResponseTimeout(responseTimeout);
            }
            // defaultHeaders + client.headers
            LinkedHashMap<String, List<String>> total = headers.stream()
                    .collect(toMap(Header::getKey, Header::getValues, (oldV, newV) -> newV, LinkedHashMap::new));
            for (Header header : client.getHeaders()) {
                total.put(header.getKey(), header.getValues());
            }
            List<Header> mergedHeaders = total.entrySet().stream()
                    .map(e -> new Header(e.getKey(), e.getValue()))
                    .collect(toList());
            client.setHeaders(mergedHeaders);
        }
    }

    HttpClientsProperties.Client defaultClient() {
        return new Client("__default__", baseUrl, responseTimeout, headers);
    }
}
