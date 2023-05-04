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
    private Long responseTimeout = 5000L;
    /**
     * Default headers, will be added to all the requests.
     */
    private List<Header> headers = new ArrayList<>();

    private List<Channel> channels = new ArrayList<>();

    /**
     * Whether to convert Java bean to query parameters, default value is {@code true}.
     */
    private boolean beanToQuery = true;

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
        private String baseUrl;
        private Long responseTimeout;
        private List<Header> headers = new ArrayList<>();
        private List<String> clients = new ArrayList<>();
        private List<Class<?>> classes = new ArrayList<>();
    }
}
