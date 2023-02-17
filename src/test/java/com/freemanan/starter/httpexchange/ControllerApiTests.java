package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
public class ControllerApiTests {

    @Test
    void userApiFirst_whenHaveControllerAndApiBeans() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Config.class)
                .web(WebApplicationType.NONE)
                .run();
        long count = ctx.getBeanProvider(FooApi.class).stream().count();
        assertThat(count).isEqualTo(2);

        Config c = ctx.getBean(Config.class);
        assertThat(c.fooApi instanceof FooApi).isTrue();
        assertThat(c.fooApi instanceof FooController).isFalse();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    @Import(FooController.class)
    public static class Config {

        @Autowired
        public FooApi fooApi;
    }

    @HttpExchange("http://localhost:8080/foo")
    interface FooApi {

        @GetExchange("/{id}")
        String getById(@PathVariable String id);
    }

    @RestController("/foo")
    static class FooController implements FooApi {

        @Override
        @GetMapping("/{id}")
        public String getById(@PathVariable String id) {
            return "foo: " + id;
        }
    }
}
