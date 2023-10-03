package com.freemanan.starter.httpexchange;

import static com.freemanan.cr.core.anno.Verb.ADD;
import static com.freemanan.starter.Dependencies.springBootVersion;
import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.PortGetter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class ExtendTests {

    @Test
    @ClasspathReplacer({
        @Action(verb = ADD, value = "org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion)
    })
    void userApiFirst_whenHaveControllerAndApiBeans() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .profiles("ControllerApiTests")
                .properties("server.port=" + port)
                .run();
        assertThat(ctx.getBeanProvider(FooApi.class)).hasSize(2);

        FooApi fooApi = ctx.getBean(FooApi.class);
        assertThat(fooApi).isNotInstanceOf(FooController.class);

        assertThat(fooApi.getById("1")).isEqualTo(new Foo("1", "foo"));

        // Can't pass Object as query param by default,
        // but we have QueryArgumentResolver to resolve it,
        // if no QueryArgumentResolver, it will throw IllegalStateException
        // assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> fooApi.findAll(new Foo("1",
        // "foo1")));

        ctx.close();
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
    @RequestMapping("/foo")
    static class FooController implements FooApi {

        @Override
        @GetMapping("/{id}")
        public Foo getById(@PathVariable String id) {
            return new Foo(id, "foo");
        }

        @Override
        @GetMapping
        public List<Foo> findAll(Foo foo) {
            return List.of(new Foo("1", "foo1"));
        }
    }
}
