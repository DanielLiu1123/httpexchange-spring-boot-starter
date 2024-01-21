package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.danielliu1123.PortGetter;
import org.junit.jupiter.api.Test;
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
class BaseUrlTests {

    @Test
    void testDefaultBaseUrl() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(BaseUrlController.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testNoBaseUrl() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(BaseUrlController.class)
                .properties("server.port=" + port)
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URI is not absolute");

        ctx.close();
    }

    @Test
    void testBaseUrl_whenClientHasBaseUrl_thenOverrideDefaultBaseUrl() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(BaseUrlController.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].base-url=localhost:" + (port + 1))
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].clients[0]=BaseUrlApi")
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10)).isInstanceOf(ResourceAccessException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = BaseUrlApi.class)
    @RestController
    static class BaseUrlController implements BaseUrlApi {
        @Override
        public String delay(int delay) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "delay " + delay;
        }
    }

    interface BaseUrlApi {

        @GetExchange("/delay/{delay}")
        String delay(@PathVariable int delay);
    }
}
