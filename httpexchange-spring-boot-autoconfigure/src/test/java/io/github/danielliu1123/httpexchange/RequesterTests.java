package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.PortGetter.availablePort;
import static io.github.danielliu1123.httpexchange.HttpExchangeClientHttpRequestInterceptor.REQUEST_TIMEOUT_HEADER;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.PREFIX;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * {@link RequestConfigurator} tester.
 *
 * @author Freeman
 */
class RequesterTests {

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testRequester(String clientType) {
        int port = availablePort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties(PREFIX + ".read-timeout=100")
                .properties(PREFIX + ".client-type=" + clientType)
                .properties(PREFIX + ".base-url=localhost:" + port)
                .run();

        Api api = ctx.getBean(Api.class);

        assertThatCode(() -> api.delay(1)).doesNotThrowAnyException();
        assertThatCode(() -> api.delay(120))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("timed out");

        // withTimeout
        assertThatCode(() -> Requester.create().withTimeout(150).call(() -> api.delay(100)))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                        Requester.create().withTimeout(150).withTimeout(100).call(() -> api.delay(100)))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("timed out");

        // addHeader
        assertThat(Requester.create().addHeader("foo1", "bar1").call(() -> api.delay(1)))
                .hasEntrySatisfying("foo1", v -> assertThat(v).contains("bar1"));
        assertThat(Requester.create()
                        .addHeader("foo1", "bar1")
                        .addHeader("foo2", "bar2")
                        .call(() -> api.delay(1)))
                .hasEntrySatisfying("foo1", v -> assertThat(v).contains("bar1"))
                .hasEntrySatisfying("foo2", v -> assertThat(v).contains("bar2"));

        // addHeader + withTimeout
        assertThat(Requester.create()
                        .addHeader("foo1", "bar1")
                        .addHeader("foo2", "bar2", "bar22")
                        .withTimeout(150)
                        .call(() -> api.delay(100)))
                .hasEntrySatisfying("foo1", v -> assertThat(v).contains("bar1"))
                .hasEntrySatisfying("foo2", v -> assertThat(v).contains("bar2", "bar22"))
                .doesNotContainKey(REQUEST_TIMEOUT_HEADER.toLowerCase());

        assertThatCode(() -> Requester.create()
                        .addHeader("foo1", "bar1")
                        .withTimeout(150)
                        .withTimeout(100)
                        .call(() -> api.delay(100)))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("timed out");

        // async
        Requester requester = Requester.create().addHeader("foo1", "bar1").withTimeout(1000);
        runAsync(() -> assertThat(requester.addHeader("foo2", "bar2").call(() -> api.delay(0)))
                        .containsKey("foo1")
                        .containsKey("foo2"))
                .join();

        ctx.close();
    }

    interface Api extends RequestConfigurator<Api> {
        @GetExchange("/api")
        Map<String, List<String>> delay(@RequestParam("delay") int delay);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    @RestController
    static class Cfg {

        @SneakyThrows
        @RequestMapping("/api")
        public Map<String, List<String>> delay(@RequestParam("delay") int delay, @RequestHeader HttpHeaders headers) {
            Thread.sleep(delay);
            return Map.copyOf(headers);
        }
    }
}
