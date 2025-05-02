package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.CancellationException;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class TimeoutTests {

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT"})
    void testDefaultTimeout_whenExceed(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.settings.read-timeout=10ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            // see org.springframework.http.client.JdkClientHttpRequest.TimeoutHandler
            assertThatCode(() -> api.delay(200)).isInstanceOf(CancellationException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"WEB_CLIENT"})
    void testDefaultTimeout_whenExceedUsingWebClient_thenTimeoutException(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.reactiveclient.settings.read-timeout=10ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(200))
                    .hasCauseInstanceOf(HttpTimeoutException.class)
                    .hasMessageContaining("timed out");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT"})
    void testDefaultTimeout_whenNotExceed(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.settings.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(20)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT"})
    void testTimeout_whenNotExceed(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties("spring.http.client.settings.read-timeout=100ms")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].base-url=http://localhost:" + port)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].clients[0]=DelayApi")
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].read-timeout=300")
                .run()) {
            DelayApi api = ctx.getBean(DelayApi.class);

            assertThatCode(() -> api.delay(200)).doesNotThrowAnyException();
        }
    }

    interface DelayApi {
        @GetExchange("/delay/{delay}")
        String delay(@PathVariable int delay);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = DelayApi.class)
    @RestController
    static class TimeoutConfig implements DelayApi {

        @Override
        @SneakyThrows
        public String delay(int delay) {
            Thread.sleep(delay);
            return "delayed " + delay + "ms";
        }
    }
}
