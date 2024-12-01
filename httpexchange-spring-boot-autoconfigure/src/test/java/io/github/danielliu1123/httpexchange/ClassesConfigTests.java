package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class ClassesConfigTests {

    @Test
    void clientClassConfig() {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(FooController.class)
                .profiles("ClassesConfigTests")
                .properties("server.port=" + port)
                .run()) {

            FooApi fooApi = ctx.getBean(FooApi.class);

            assertThat(fooApi.getById("1")).isEqualTo("foo");
        }
    }

    @HttpExchange("/foo")
    public interface FooApi {

        @GetExchange("/{id}")
        String getById(@PathVariable String id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    static class FooController implements FooApi {

        @Override
        public String getById(String id) {
            return "foo";
        }
    }
}
