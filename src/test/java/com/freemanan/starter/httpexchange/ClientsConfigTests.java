package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.PortFinder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class ClientsConfigTests {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "foo-api",
                "FoOApI",
                "com.freemanan.starter.httpexchange.ClientsConfigTests.FooApi",
                "com.freemanan.starter.httpexchange.ClientsConfigTests$FooApi",
                "com.**",
            })
    void notThrow_whenClientMatchesCanonicalClassName(String client) {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .web(WebApplicationType.NONE)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".channels[0].base-url=${server.port}")
                .properties(HttpClientsProperties.PREFIX + ".channels[0].clients[0]=" + client)
                .run();

        assertThatCode(() -> ctx.getBean(FooApi.class)).doesNotThrowAnyException();

        ctx.close();
    }

    @HttpExchange("/foo")
    public interface FooApi {

        @GetExchange("/{id}")
        String getById(@PathVariable String id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    static class Cfg {}
}