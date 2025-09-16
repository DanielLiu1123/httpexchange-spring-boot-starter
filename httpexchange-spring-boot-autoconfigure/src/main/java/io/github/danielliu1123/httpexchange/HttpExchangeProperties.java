package io.github.danielliu1123.httpexchange;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.http.client.autoconfigure.reactive.HttpReactiveClientProperties;
import org.springframework.boot.restclient.autoconfigure.AbstractRestClientProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.annotation.HttpExchange;

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
     *
     * @since 3.2.0
     */
    private Set<Class<?>> clients = new LinkedHashSet<>();
    /**
     * Default base url, 'http' scheme can be omitted.
     *
     * <p> If loadbalancer is enabled, this value means the service id.
     *
     * <ul>
     *     <li> localhost:8080 </li>
     *     <li> http://localhost:8080 </li>
     *     <li> https://localhost:8080 </li>
     *     <li> localhost:8080/api </li>
     *     <li> user(service id) </li>
     * </ul>
     */
    @Nullable
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
     * Client Type, if not specified, an appropriate client type will be set.
     *
     * <ul>
     *     <li> Use {@link ClientType#REST_CLIENT} if none of the methods in the client return Reactive type.
     *     <li> Use {@link ClientType#WEB_CLIENT} if any method in the client returns Reactive type.
     * </ul>
     *
     * <p> In most cases, you don't need to explicitly specify the client type.
     *
     * @see ClientType
     * @since 3.2.0
     */
    @Nullable
    private ClientType clientType;
    /**
     * whether to process {@link RequestMapping} based annotation,
     * default {@code false}.
     *
     * <p color="red"> Recommending to use {@link HttpExchange} instead of {@link RequestMapping}.
     *
     * @since 3.2.0
     */
    private boolean requestMappingSupportEnabled = false;
    /**
     * Whether to check unused configuration, default {@code true}.
     *
     * @since 3.2.0
     */
    private boolean warnUnusedConfigEnabled = true;
    /**
     * Whether to enable loadbalancer, default {@code true}.
     *
     * <p> Prerequisites:
     * <ul>
     *     <li> {@code spring-cloud-starter-loadbalancer} dependency in the classpath.</li>
     *     <li> {@code spring.cloud.loadbalancer.enabled=true}</li>
     * </ul>
     *
     * @since 3.2.0
     */
    private boolean loadbalancerEnabled = true;
    /**
     * Whether to enable http client reuse, default {@code true}.
     *
     * <p> Same {@link Channel} configuration will share the same http client if enabled.
     *
     * @since 3.2.2
     */
    private boolean httpClientReuseEnabled = true;

    /**
     * @param key    Header key.
     * @param values Header values.
     */
    public record Header(String key, List<String> values) {
        public Header {
            values = List.copyOf(values);
        }
    }

    @Override
    public void afterPropertiesSet() {
        merge();
    }

    /**
     * Merge default configuration to channels configuration.
     */
    void merge() {
        PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        for (Channel chan : channels) {
            mapper.from(baseUrl).when(e -> isNull(chan.getBaseUrl())).to(chan::setBaseUrl);
            mapper.from(clientType).when(e -> isNull(chan.getClientType())).to(chan::setClientType);
            mapper.from(loadbalancerEnabled)
                    .when(e -> isNull(chan.getLoadbalancerEnabled()))
                    .to(chan::setLoadbalancerEnabled);
            mapper.from(httpClientReuseEnabled)
                    .when(e -> isNull(chan.getHttpClientReuseEnabled()))
                    .to(chan::setHttpClientReuseEnabled);

            // defaultHeaders + chan.headers
            LinkedHashMap<String, List<String>> total = headers.stream()
                    .collect(toMap(Header::key, Header::values, (oldV, newV) -> oldV, LinkedHashMap::new));
            for (Header header : chan.getHeaders()) {
                total.put(header.key(), header.values());
            }
            List<Header> mergedHeaders = total.entrySet().stream()
                    .map(e -> new Header(e.getKey(), e.getValue()))
                    .toList();
            chan.setHeaders(mergedHeaders);
        }
    }

    HttpExchangeProperties.Channel defaultClient() {
        return new Channel(
                null,
                baseUrl,
                headers,
                clientType,
                null,
                null,
                null,
                loadbalancerEnabled,
                httpClientReuseEnabled,
                null,
                List.of(),
                List.of());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        /**
         * Optional channel name.
         */
        @Nullable
        private String name;
        /**
         * Base url, use {@link HttpExchangeProperties#baseUrl} if not set.
         */
        @Nullable
        private String baseUrl;
        /**
         * Default headers will be merged with {@link HttpExchangeProperties#headers}.
         */
        private List<Header> headers = new ArrayList<>();
        /**
         * Client type, use {@link HttpExchangeProperties#clientType} if not set.
         *
         * @see ClientType
         */
        @Nullable
        private ClientType clientType;
        /**
         * Redirects configuration.
         *
         *
         * <p> If not set, default redirects will be used:
         * <p> For {@link ClientType#REST_CLIENT}, use {@code spring.http.client.redirects}.
         * <p> For {@link ClientType#WEB_CLIENT}, use {@code spring.http.reactiveclient.redirects}.
         *
         * @see HttpRedirects
         * @since 3.5.0
         */
        @Nullable
        private HttpRedirects redirects;
        /**
         * Connection timeout duration, specified in milliseconds.
         *
         * <p> If not set, default connection timeout will be used:
         * <p> For {@link ClientType#REST_CLIENT}, use {@code spring.http.client.connect-timeout}.
         * <p> For {@link ClientType#WEB_CLIENT}, use {@code spring.http.reactiveclient.connect-timeout}.
         *
         * @see HttpClientProperties#getConnectTimeout()
         * @see HttpReactiveClientProperties#getConnectTimeout()
         * @since 3.2.0
         */
        @Nullable
        private Integer connectTimeout;
        /**
         * Read timeout duration, specified in milliseconds.
         *
         * <p> If not set, default read timeout will be used:
         * <p> For {@link ClientType#REST_CLIENT}, use {@code spring.http.client.read-timeout}.
         * <p> For {@link ClientType#WEB_CLIENT}, use {@code spring.http.reactiveclient.read-timeout}.
         *
         * @see HttpClientProperties#getReadTimeout()
         * @see HttpReactiveClientProperties#getReadTimeout()
         * @since 3.2.0
         */
        @Nullable
        private Integer readTimeout;
        /**
         * Whether to enable loadbalancer, use {@link HttpExchangeProperties#loadbalancerEnabled} if not set.
         *
         * @see HttpExchangeProperties#loadbalancerEnabled
         * @since 3.2.0
         */
        @Nullable
        private Boolean loadbalancerEnabled;
        /**
         * Whether to enable http client reuse, use {@link HttpExchangeProperties#httpClientReuseEnabled} if not set.
         *
         * @see HttpExchangeProperties#httpClientReuseEnabled
         * @since 3.2.2
         */
        @Nullable
        private Boolean httpClientReuseEnabled;
        /**
         * SSL configuration.
         *
         * <p> If not set, default ssl configuration will be used:
         * <p> For {@link ClientType#REST_CLIENT}, use {@code spring.http.client.ssl},
         * <p> For {@link ClientType#WEB_CLIENT}, use {@code spring.http.reactiveclient.ssl}.
         *
         * @see HttpClientProperties#getSsl()
         * @see HttpReactiveClientProperties#getSsl()
         * @since 3.4.1
         */
        @Nullable
        private Ssl ssl;
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
         * <p> This feature needs {@code spring-cloud-context} dependency in the classpath.
         *
         * <p color="orange"> NOTE: This feature is not supported by native image.
         *
         * @see <a href="https://github.com/spring-cloud/spring-cloud-release/wiki/AOT-transformations-and-native-image-support#refresh-scope">Refresh Scope</a>
         */
        private boolean enabled = false;
    }

    /**
     * @param bundle SSL bundle to use.
     *
     *               <p> Bundle name is configured under {@code spring.ssl} properties.
     *
     *               <p> See configuration properties under {@code spring.ssl}.
     * @see AbstractRestClientProperties.Ssl
     */
    public record Ssl(String bundle) {}

    public enum ClientType {
        /**
         * @see RestClient
         */
        REST_CLIENT,
        /**
         * @see WebClient
         */
        WEB_CLIENT,
    }
}
