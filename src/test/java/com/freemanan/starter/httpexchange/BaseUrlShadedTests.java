package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.PortFinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * @author Freeman
 */
class BaseUrlShadedTests {

    @Test
    void testDefaultBaseUrl() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(BaseUrlController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testNoBaseUrl() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(BaseUrlController.class)
                .properties("server.port=" + port)
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10))
                .isInstanceOf(WebClientRequestException.class)
                .hasMessageContaining("Connection refused:");

        ctx.close();
    }

    @Test
    void testBaseUrl_whenClientHasBaseUrl_thenOverrideDefaultBaseUrl() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(BaseUrlController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .properties(HttpClientsProperties.PREFIX + ".channels[0].base-url=localhost:" + (port + 1))
                .properties(HttpClientsProperties.PREFIX + ".channels[0].clients[0]=BaseUrlApi")
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10)).isInstanceOf(RuntimeException.class);

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

        @GetMapping("/delay/{delay}")
        String delay(@PathVariable int delay);
    }
}
