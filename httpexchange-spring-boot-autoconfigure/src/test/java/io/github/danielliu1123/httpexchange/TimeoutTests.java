package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.danielliu1123.PortGetter;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".read-timeout=200")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatExceptionOfType(ResourceAccessException.class)
                .isThrownBy(() -> api.delay(300))
                .withMessageContaining("request timed out");

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testDefaultTimeout_whenNotExceed() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".read-timeout=200")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + ClientType.WEB_CLIENT)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(100)).doesNotThrowAnyException();

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testTimeout_whenExceed() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".read-timeout=200")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + ClientType.WEB_CLIENT)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].base-url=http://localhost:" + port)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].clients[0]=DelayApi")
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].read-timeout=600")
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(400)).doesNotThrowAnyException();

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WEB_CLIENT"})
    void testTimeout_whenUsingWebClient_thenTimeoutConfigIsNotWork(String clientType) {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".read-timeout=200")
                .properties(HttpExchangeProperties.PREFIX + ".client-type=" + clientType)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(300)).doesNotThrowAnyException();

        ctx.close();
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
