package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
public class TimeoutTests {

    @Test
    void testDefaultTimeout_whenExceed() {
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .web(WebApplicationType.NONE)
                .properties(HttpClientsProperties.PREFIX + ".default-response-timeout=800")
                .run();
        TimeoutApi api = ctx.getBean(TimeoutApi.class);

        assertThatCode(() -> api.delay(1)).hasRootCauseInstanceOf(TimeoutException.class);

        ctx.close();
    }

    @Test
    void testDefaultTimeout_whenNotExceed() {
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .web(WebApplicationType.NONE)
                .properties(HttpClientsProperties.PREFIX + ".default-response-timeout=4000")
                .run();
        TimeoutApi api = ctx.getBean(TimeoutApi.class);

        assertThatCode(() -> api.delay(1)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testTimeout_whenExceed() {
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .web(WebApplicationType.NONE)
                .properties(HttpClientsProperties.PREFIX + ".timeout=800")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].name=TimeoutApi")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].response-timeout=4000")
                .run();
        TimeoutApi api = ctx.getBean(TimeoutApi.class);

        assertThatCode(() -> api.delay(1)).doesNotThrowAnyException();

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = TimeoutApi.class)
    static class TimeoutConfig {}

    @HttpExchange("https://httpbin.org")
    interface TimeoutApi {

        @GetExchange("/delay/{delay}")
        void delay(@PathVariable int delay);
    }
}
