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
     * Whether to enable http exchange autoconfiguration, default {@code true}.
     */
    private boolean enabled = true;
    /**
     * Base packages to scan, use {@link EnableExchangeClients#basePackages} first if configured.
     */
    private List<String> basePackages = new ArrayList<>();
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
    private Long responseTimeout = 5000L;
    /**
     * Default headers, will be added to all the requests.
     */
    private List<Header> headers = new ArrayList<>();

    private List<Channel> channels = new ArrayList<>();

    /**
     * Whether to convert Java bean to query parameters, default value is {@code false}.
     */
    private boolean beanToQuery = false;

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
        merge();
    }

    /**
     * Merge default configuration to channels configuration.
     */
    public void merge() {
        for (Channel chan : channels) {
            if (chan.getBaseUrl() == null) {
                chan.setBaseUrl(baseUrl);
            }
            if (chan.getResponseTimeout() == null) {
                chan.setResponseTimeout(responseTimeout);
            }
            // defaultHeaders + chan.headers
            LinkedHashMap<String, List<String>> total = headers.stream()
                    .collect(toMap(Header::getKey, Header::getValues, (oldV, newV) -> oldV, LinkedHashMap::new));
            for (Header header : chan.getHeaders()) {
                total.put(header.getKey(), header.getValues());
            }
            List<Header> mergedHeaders = total.entrySet().stream()
                    .map(e -> new Header(e.getKey(), e.getValue()))
                    .toList();
            chan.setHeaders(mergedHeaders);
        }
    }

    HttpClientsProperties.Channel defaultClient() {
        return new Channel(baseUrl, responseTimeout, headers, List.of(), List.of());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        // TODO(Freeman): add name property to identify channel?
        /**
         * Base url, use {@link HttpClientsProperties#baseUrl} if not set.
         */
        private String baseUrl;
        /**
         * Response timeout, in milliseconds, use {@link HttpClientsProperties#responseTimeout} if not set.
         */
        private Long responseTimeout;
        /**
         * Default headers, will be merged with {@link HttpClientsProperties#headers}.
         */
        private List<Header> headers = new ArrayList<>();
        /**
         * Exchange Clients to apply this channel.
         *
         * <p> e.g. client {@code com.example.client.ExampleClient} can be identified by
         * <ul>
         *     <li> {@code ExampleClient}, {@code exampleClient}, {@code example-client} (Class simple name)
         *     <li> {@code com.example.client.ExampleClient} (Class canonical name)
         *     <li> {@code com.**.*Client}, {@code com.example.**} (<a href="https://stackoverflow.com/questions/2952196/ant-path-style-patterns">Ant style pattern</a>)
         * </ul>
         *
         * <p> This is a more flexible alternative to {@link HttpClientsProperties.Channel#classes}.
         *
         * @see Class#getCanonicalName()
         * @see org.springframework.util.AntPathMatcher
         */
        private List<String> clients = new ArrayList<>();
        /**
         * Exchange Client classes to apply this channel.
         *
         * <p> This is a more IDE-friendly alternative to {@link HttpClientsProperties.Channel#clients}.
         */
        private List<Class<?>> classes = new ArrayList<>();
    }
}
