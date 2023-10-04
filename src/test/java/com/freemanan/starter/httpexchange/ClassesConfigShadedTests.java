package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.starter.PortGetter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Freeman
 */
class ClassesConfigShadedTests {

    @Test
    void clientClassConfig() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .properties("server.port=" + port)
                .properties("http-exchange.channels[0].base-url=localhost:${server.port}")
                .properties("http-exchange.channels[0].classes[0]=" + FooApi.class.getCanonicalName())
                .run();

        FooApi fooApi = ctx.getBean(FooApi.class);

        assertThat(fooApi.getById("1")).isEqualTo("foo");

        ctx.close();
    }

    @RequestMapping("/foo")
    public interface FooApi {

        @GetMapping("/{id}")
        String getById(@PathVariable String id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    @RequestMapping("/foo")
    static class FooController implements FooApi {

        @Override
        @GetMapping("/{id}")
        public String getById(@PathVariable String id) {
            return "foo";
        }
    }
}
