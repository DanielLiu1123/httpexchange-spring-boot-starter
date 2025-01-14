package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.github.danielliu1123.httpexchange.shaded.requestfactory.EnhancedJdkClientHttpRequestFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.client.HttpClientProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class TimeoutTests {

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testDefaultTimeout_whenExceed(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatExceptionOfType(ResourceAccessException.class)
                    .isThrownBy(() -> api.delay(200))
                    .withMessageContaining("timed out");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"WEB_CLIENT"})
    void testDefaultTimeout_whenExceedUsingWebClient_thenTimeoutException(String clientType) {
        int port = findAvailableTcpPort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(120)).hasCauseInstanceOf(TimeoutException.class);

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testDefaultTimeout_whenNotExceed(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(20)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testTimeout_whenNotExceed(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].base-url=http://localhost:" + port)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].clients[0]=DelayApi")
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].read-timeout=300")
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(200)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testTimeoutForSingleRequest_whenUsingBlockingClient_thenWorksFine(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {

            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(120))
                    .isInstanceOf(ResourceAccessException.class)
                    .hasMessageContaining("timed out");
            assertThatCode(() -> api.withTimeout(200).delay(120)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"WEB_CLIENT"})
    void testTimeoutForSingleRequest_whenUsingReactiveClient_thenNotWork(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {

            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(120)).doesNotThrowAnyException();
            assertThatCode(() -> api.withTimeout(50).delay(120)).doesNotThrowAnyException(); // Not work
        }
    }

    interface DelayApi extends RequestConfigurator<DelayApi> {
        @GetExchange("/delay/{delay}")
        String delay(@PathVariable int delay);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = DelayApi.class)
    @RestController
    static class TimeoutConfig implements DelayApi {

        @Bean
        HttpClientCustomizer.RestClientCustomizer restClientCustomizer(HttpClientProperties httpClientProperties) {
            return (client, channel) -> {
                EnhancedJdkClientHttpRequestFactory requestFactory = new EnhancedJdkClientHttpRequestFactory();
                var readTimeout = Optional.ofNullable(channel.getReadTimeout())
                        .map(Duration::ofMillis)
                        .orElse(httpClientProperties.getReadTimeout());
                Optional.ofNullable(readTimeout).ifPresent(requestFactory::setReadTimeout);
                client.requestFactory(requestFactory);
            };
        }

        @Bean
        HttpClientCustomizer.RestTemplateCustomizer restTemplateCustomizer(HttpClientProperties httpClientProperties) {
            return (client, channel) -> {
                EnhancedJdkClientHttpRequestFactory requestFactory = new EnhancedJdkClientHttpRequestFactory();
                var readTimeout = Optional.ofNullable(channel.getReadTimeout())
                        .map(Duration::ofMillis)
                        .orElse(httpClientProperties.getReadTimeout());
                Optional.ofNullable(readTimeout).ifPresent(requestFactory::setReadTimeout);
                client.setRequestFactory(requestFactory);
            };
        }

        @Override
        @SneakyThrows
        public String delay(int delay) {
            Thread.sleep(delay);
            return "delayed " + delay + "ms";
        }
    }
}
