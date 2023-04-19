package com.freemanan.starter.httpexchange;

import static java.util.stream.Collectors.toMap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Http Clients Configuration Properties.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(HttpClientsProperties.PREFIX)
public class HttpClientsProperties implements InitializingBean {
    public static final String PREFIX = "http-exchange";

    /**
     * Default base url.
     *
     * <p> e.g. {@code localhost:8080}, {@code http://localhost:8080}, {@code https://localhost:8080}
     */
    private String baseUrl;
    /**
     * Default response timeout, in milliseconds, default value is {@code 5000}.
     *
     * @see HttpServiceProxyFactory.Builder#blockTimeout(Duration)
     */
    private Long responseTimeout;
    /**
     * Default headers, will be added to all the requests.
     */
    private List<Header> headers = new ArrayList<>();

    private List<Client> clients = new ArrayList<>();

    /**
     * Whether to convert Java bean to query parameters, default value is {@code true}.
     */
    private boolean beanToQuery = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Client {
        /**
         * Client name, identify a client.
         *
         * <p> Can be simple name, or full class name.
         *
         * <p> e.g. {@code FooApi}, {@code com.example.FooApi}, {@code foo-api} can be used to identify client {@code com.example.FooApi}.
         */
        private String name;
        /**
         * Client class, identify a client, must be an interface.
         *
         * <p> This a more IDE friendly way to identify a client.
         *
         * <p> Properties {@link #name} and clazz used to identify a client, use clazz first if both set.
         */
        private Class<?> clientClass;
        /**
         * Base url, use {@link HttpClientsProperties#baseUrl} if not set.
         */
        private String baseUrl;
        /**
         * Response timeout, in milliseconds, use {@link HttpClientsProperties#responseTimeout} if not set.
         */
        private Long responseTimeout;
        /**
         * Headers to be added to the requests, use {@link HttpClientsProperties#headers} if not set.
         */
        private List<Header> headers = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        /**
         * Header key.
         */
        private String key;
        /**
         * Header values.
         */
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
                    .collect(toMap(Header::getKey, Header::getValues, (oldV, newV) -> oldV, LinkedHashMap::new));
            for (Header header : client.getHeaders()) {
                total.put(header.getKey(), header.getValues());
            }
            List<Header> mergedHeaders = total.entrySet().stream()
                    .map(e -> new Header(e.getKey(), e.getValue()))
                    .toList();
            client.setHeaders(mergedHeaders);
        }
    }

    HttpClientsProperties.Client defaultClient() {
        return new Client("__default__", null, baseUrl, responseTimeout, headers);
    }
}
