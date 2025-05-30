package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class ClientTypeTests {

    @ParameterizedTest
    @ValueSource(strings = {"rest_client", "web_client"})
    void testRestClient(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("http-exchange.client-type=" + clientType)
                .properties("http-exchange.base-url=localhost:" + port)
                .run()) {

            Api api = ctx.getBean(Api.class);

            assertThat(api.hi()).isEqualTo("Hi");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    @RestController
    static class Cfg implements Api {

        @Override
        public String hi() {
            return "Hi";
        }
    }

    @HttpExchange
    interface Api {

        @GetExchange("/hi")
        String hi();
    }
}
