package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
public class BaseUrlTests {

    @Test
    void testDefaultBaseUrl() {
        var ctx = new SpringApplicationBuilder(BaseUrlConfig.class)
                .web(WebApplicationType.NONE)
                .properties(HttpClientsProperties.PREFIX + ".base-url=https://httpbin.org")
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(1)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testNoBaseUrl() {
        var ctx = new SpringApplicationBuilder(BaseUrlConfig.class)
                .web(WebApplicationType.NONE)
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(1)).isInstanceOf(IllegalArgumentException.class);

        ctx.close();
    }

    @Test
    void testBaseUrl_whenClientHasBaseUrl_thenOverrideDefaultBaseUrl() {
        var ctx = new SpringApplicationBuilder(BaseUrlConfig.class)
                .web(WebApplicationType.NONE)
                .properties(HttpClientsProperties.PREFIX + ".base-url=https://httpbin.org")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].name=BaseUrlApi")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].base-url=https://httpbinxxxxxxxx.org")
                .run();
        BaseUrlApi api = ctx.getBean(BaseUrlApi.class);

        assertThatCode(() -> api.delay(1)).hasCauseInstanceOf(SSLHandshakeException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = BaseUrlApi.class)
    static class BaseUrlConfig {}

    interface BaseUrlApi {

        @GetExchange("/delay/{delay}")
        void delay(@PathVariable int delay);
    }
}
