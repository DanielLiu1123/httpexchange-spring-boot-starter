package io.github.danielliu1123.httpexchange;

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
@ConfigurationProperties(HttpExchangeProperties.PREFIX)
public class HttpExchangeProperties implements InitializingBean {
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
     * Exchange client interfaces to register as beans, use {@link EnableExchangeClients#clients} first if configured.
     */
    private Set<Class<?>> clients = new LinkedHashSet<>();
    /**
     * Default base url, 'http' scheme can be omitted.
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
     * Client type, default {@link ClientType#REST_CLIENT}.
     *
     * <p color="orange"> NOTE: the {@link #connectTimeout} and {@link #readTimeout} settings are not supported by {@link ClientType#WEB_CLIENT}.
     *
     * @see ClientType
     * @since 3.2.0
     */
    private ClientType clientType = ClientType.REST_CLIENT;
    /**
     * whether to process {@link org.springframework.web.bind.annotation.RequestMapping} based annotation,
     * default {@code false}.
     *
     * <p color="red"> Recommending to use {@link org.springframework.web.service.annotation.HttpExchange} instead of {@link org.springframework.web.bind.annotation.RequestMapping}.
     *
     * @since 3.2.0
     */
    private boolean requestMappingSupportEnabled = false;
    /**
     * Connect timeout duration, specified in milliseconds.
     * Negative, zero, or null values indicate that the timeout is not set.
     *
     * @since 3.2.0
     */
    private Integer connectTimeout;
    /**
     * Read timeout duration, specified in milliseconds.
     * Negative, zero, or null values indicate that the timeout is not set.
     *
     * @since 3.2.0
     */
    private Integer readTimeout;
    /**
     * Whether to check unused configuration, default {@code true}.
     *
     * @since 3.2.0
     */
    private boolean warnUnusedConfig = true;

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
            if (chan.getClientType() == null) {
                chan.setClientType(clientType);
            }
            if (chan.getConnectTimeout() == null) {
                chan.setConnectTimeout(connectTimeout);
            }
            if (chan.getReadTimeout() == null) {
                chan.setReadTimeout(readTimeout);
            }
        }
    }

    HttpExchangeProperties.Channel defaultClient() {
        return new Channel(null, baseUrl, headers, clientType, connectTimeout, readTimeout, List.of(), List.of());
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
         * Base url, 'http' scheme can be omitted, use {@link HttpExchangeProperties#baseUrl} if not set.
         */
        private String baseUrl;
        /**
         * Default headers, will be merged with {@link HttpExchangeProperties#headers}.
         */
        private List<Header> headers = new ArrayList<>();
        /**
         * Client type, use {@link HttpExchangeProperties#clientType} if not set.
         *
         * <p color="orange"> NOTE: the {@link #connectTimeout} and {@link #readTimeout} settings are not supported by {@link ClientType#WEB_CLIENT}.
         *
         * @see ClientType
         */
        private ClientType clientType;
        /**
         * Connection timeout duration, specified in milliseconds.
         * Negative, zero, or null values indicate that the timeout is not set.
         *
         * <p> Use {@link HttpExchangeProperties#connectTimeout} if not set.
         *
         * @since 3.2.0
         */
        private Integer connectTimeout;
        /**
         * Read timeout duration, specified in milliseconds.
         * Negative, zero, or null values indicate that the timeout is not set.
         *
         * <p> Use {@link HttpExchangeProperties#readTimeout} if not set.
         *
         * @since 3.2.0
         */
        private Integer readTimeout;
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
         * <p> This is a more flexible alternative to {@link HttpExchangeProperties.Channel#classes}.
         *
         * @see Class#getCanonicalName()
         * @see org.springframework.util.AntPathMatcher
         */
        private List<String> clients = new ArrayList<>();
        /**
         * Exchange Client classes to apply this channel.
         *
         * <p> This is a more IDE-friendly alternative to {@link HttpExchangeProperties.Channel#clients}.
         */
        private List<Class<?>> classes = new ArrayList<>();
    }

    @Data
    public static class Refresh {
        public static final String PREFIX = HttpExchangeProperties.PREFIX + ".refresh";
        /**
         * Whether to enable refresh exchange clients, default {@code false}.
         *
         * <p> NOTE: this feature needs {@code spring-cloud-context} dependency in the classpath.
         */
        private boolean enabled = false;
    }
}
