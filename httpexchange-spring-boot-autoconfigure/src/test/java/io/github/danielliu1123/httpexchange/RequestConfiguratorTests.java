package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.HttpExchangeClientHttpRequestInterceptor.REQUEST_TIMEOUT_HEADER;
import static io.github.danielliu1123.httpexchange.HttpExchangeProperties.PREFIX;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.danielliu1123.PortGetter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * {@link RequestConfigurator} tester.
 *
 * @author Freeman
 */
class RequestConfiguratorTests {

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE"})
    void testRequestConfigurator(String clientType) {
        int port = PortGetter.availablePort();
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
                .hasMessageContaining("request timed out");

        // withTimeout
        assertThatCode(() -> api.withTimeout(150).delay(100)).doesNotThrowAnyException();
        assertThatCode(() -> api.withTimeout(150).withTimeout(100).delay(100))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("request timed out");

        // addHeader
        assertThat(api.addHeader("foo1", List.of("bar1")).delay(1))
                .hasEntrySatisfying("foo1", v -> assertThat(v).isEqualTo("bar1"));
        assertThat(api.addHeader("foo1", List.of("bar1"))
                        .addHeader("foo2", List.of("bar2"))
                        .delay(1))
                .hasEntrySatisfying("foo1", v -> assertThat(v).isEqualTo("bar1"))
                .hasEntrySatisfying("foo2", v -> assertThat(v).isEqualTo("bar2"));

        // addHeader + withTimeout
        assertThat(api.addHeader("foo1", List.of("bar1")).withTimeout(150).delay(100))
                .hasEntrySatisfying("foo1", v -> assertThat(v).isEqualTo("bar1"))
                .doesNotContainKey(REQUEST_TIMEOUT_HEADER.toLowerCase());

        assertThatCode(() -> api.addHeader("foo1", List.of("bar1"))
                        .withTimeout(150)
                        .withTimeout(100)
                        .delay(100))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("request timed out");

        // async
        Api apiForAsync = api.addHeader("foo1", List.of("bar1")).withTimeout(1000);
        runAsync(() -> assertThat(apiForAsync.addHeader("foo2", List.of("bar2")).delay(0))
                        .containsKey("foo1")
                        .containsKey("foo2"))
                .join();

        ctx.close();
    }

    interface Api extends RequestConfigurator<Api> {
        @GetExchange("/api")
        Map<String, String> delay(@RequestParam("delay") int delay);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    @RestController
    static class Cfg implements Api {

        @Autowired
        HttpServletRequest request;

        @Override
        @SneakyThrows
        public Map<String, String> delay(int delay) {
            Thread.sleep(delay);
            // return all headers
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
            return headers;
        }
    }
}
