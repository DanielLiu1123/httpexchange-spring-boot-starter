package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.Dependencies.springBootVersion;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.PortFinder;
import java.net.SocketException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
@ClasspathReplacer(@Action("org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion))
public class BaseUrlTests {

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
                .properties(HttpClientsProperties.PREFIX + ".clients[0].name=BaseUrlApi")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].base-url=localhost:" + (port + 1))
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(10))
                .hasRootCauseInstanceOf(SocketException.class)
                .hasMessageContaining("Invalid argument");

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = BaseUrlApi.class)
    @RestController
    static class BaseUrlController implements BaseUrlApi {
        @Override
        @GetMapping("/delay/{delay}")
        public void delay(int delay) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    interface BaseUrlApi {

        @GetExchange("/delay/{delay}")
        void delay(@PathVariable int delay);
    }
}
