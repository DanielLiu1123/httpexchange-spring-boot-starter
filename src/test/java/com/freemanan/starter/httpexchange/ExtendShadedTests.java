package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.PortFinder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Freeman
 */
class ExtendShadedTests {

    @Test
    void userApiFirst_whenHaveControllerAndApiBeans() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .profiles("ControllerApiTests")
                .properties("server.port=" + port)
                .run();
        long count = ctx.getBeanProvider(FooApi.class).stream().count();
        assertThat(count).isEqualTo(2);

        FooApi fooApi = ctx.getBean(FooApi.class);
        assertThat(fooApi).isNotInstanceOf(FooController.class);

        assertThat(fooApi.getById("1")).isEqualTo(new Foo("1", "foo"));

        // Can't pass Object as query param by default,
        // but we have QueryArgumentResolver to resolve it,
        // if no QueryArgumentResolver, it will throw IllegalStateException
        // assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> fooApi.findAll(new Foo("1",
        // "foo1")));

        // Default implementation not be invoked by client side
        assertThatCode(fooApi::notImplemented)
                .isInstanceOf(WebClientResponseException.NotImplemented.class)
                .hasMessageContaining("Not Implemented");

        assertThat(fooApi.implemented()).isEqualTo("OK");

        ctx.close();
    }

    record Foo(String id, String name) {}

    @RequestMapping("/foo")
    interface FooApi {

        @GetMapping("/{id}")
        Foo getById(@PathVariable String id);

        @GetMapping
        List<Foo> findAll(Foo foo);

        @GetMapping("/not-implemented")
        default String notImplemented() {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        }

        @GetMapping("/implemented")
        default String implemented() {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    static class FooController implements FooApi {

        @Override
        public Foo getById(String id) {
            return new Foo(id, "foo");
        }

        @Override
        public List<Foo> findAll(Foo foo) {
            return List.of(new Foo("1", "foo1"));
        }

        @Override
        public String implemented() {
            return "OK";
        }
    }
}
