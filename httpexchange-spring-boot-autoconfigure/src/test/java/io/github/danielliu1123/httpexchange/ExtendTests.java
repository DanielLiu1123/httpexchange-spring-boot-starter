package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielliu1123.PortGetter;
import java.util.List;
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
class ExtendTests {

    @Test
    void userApiFirst_whenHaveControllerAndApiBeans() {
        int port = PortGetter.availablePort();
        try (var ctx = new SpringApplicationBuilder(FooController.class)
                .profiles("ControllerApiTests")
                .properties("server.port=" + port)
                .run()) {
            assertThat(ctx.getBeanProvider(FooApi.class)).hasSize(2);

            FooApi fooApi = ctx.getBean(FooApi.class);
            assertThat(fooApi).isNotInstanceOf(FooController.class);

            assertThat(fooApi.getById("1")).isEqualTo(new Foo("1", "foo"));

            // Can't pass Object as query param by default,
            // but we have QueryArgumentResolver to resolve it,
            // if no QueryArgumentResolver, it will throw IllegalStateException
            // assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> fooApi.findAll(new Foo("1",
            // "foo1")));
        }
    }

    record Foo(String id, String name) {}

    @HttpExchange("/foo")
    interface FooApi {

        @GetExchange("/{id}")
        Foo getById(@PathVariable String id);

        @GetExchange
        List<Foo> findAll(Foo foo);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    static class FooController implements FooApi {

        @Override
        public Foo getById(@PathVariable String id) {
            return new Foo(id, "foo");
        }

        @Override
        public List<Foo> findAll(Foo foo) {
            return List.of(new Foo("1", "foo1"));
        }
    }
}
