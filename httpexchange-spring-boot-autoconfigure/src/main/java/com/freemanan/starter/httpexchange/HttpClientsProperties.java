package com.freemanan.starter.httpexchange;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    private Set<String> basePackages = new LinkedHashSet<>();
    /**
     * Default base url.
     *
     * <p> e.g. {@code localhost:8080}, {@code http://localhost:8080}, {@code https://localhost:8080}
     */
    private String baseUrl;
    /**
     * Default headers will be added to all the requests.
     */
    private List<Header> headers = new ArrayList<>();
    /**
     * Channels configuration.
     */
    private List<Channel> channels = new ArrayList<>();
    /**
     * Whether to convert Java bean to query parameters, default value is {@code false}.
     */
    private boolean beanToQueryEnabled = false;
    /**
     * Refresh configuration.
     */
    private Refresh refresh = new Refresh();
    /**
     * Backend type, default {@link ExchangeClientBackend#REST_CLIENT}.
     *
     * @see ExchangeClientBackend
     */
    private ExchangeClientBackend backend = ExchangeClientBackend.REST_CLIENT;
    /**
     * whether to process {@link org.springframework.web.bind.annotation.RequestMapping} based annotation,
     * default {@code false}.
     *
     * <p color="red"> Recommending to use {@link org.springframework.web.service.annotation.HttpExchange} instead of {@link org.springframework.web.bind.annotation.RequestMapping}.
     *
     * @since 3.2.0
     */
    private boolean requestMappingSupportEnabled = false;

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
            if (chan.getBackend() == null) {
                chan.setBackend(backend);
            }
        }
    }

    HttpClientsProperties.Channel defaultClient() {
        return new Channel(null, baseUrl, headers, List.of(), List.of(), backend);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        /**
         * Optional channel name.
         */
        private String name;
        /**
         * Base url, use {@link HttpClientsProperties#baseUrl} if not set.
         */
        private String baseUrl;
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
        /**
         * Backend type, use {@link HttpClientsProperties#backend} if not set.
         *
         * @see ExchangeClientBackend
         */
        private ExchangeClientBackend backend;
    }

    @Data
    public static class Refresh {
        public static final String PREFIX = HttpClientsProperties.PREFIX + ".refresh";
        /**
         * Whether to enable refresh exchange clients, default {@code false}.
         *
         * <p> NOTE: this feature needs {@code spring-cloud-context} dependency in the classpath.
         */
        private boolean enabled = false;
    }
}
