package com.freemanan.starter.httpexchange;

import static com.freemanan.cr.core.anno.Verb.ADD;
import static com.freemanan.starter.Dependencies.springBootVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.PortFinder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * {@link BeanToQueryArgumentResolver} tester.
 *
 * @author Freeman
 */
class BeanToQueryArgumentResolverTests {

    @Test
    @ClasspathReplacer({
        @Action(verb = ADD, value = "org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion)
    })
    void convertObjectPropertiesToRequestParameters() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=http://localhost:" + port)
                .run();

        FooApi fooApi = ctx.getBean(FooApi.class);

        assertThat(fooApi).isNotInstanceOf(FooController.class);

        assertThat(fooApi.findAll(new Foo("1", "foo1"))).isEqualTo(List.of(new Foo("1", "foo1")));
        assertThat(fooApi.post(new Foo("1", "foo"))).isEqualTo(new Foo("1", "foo"));
        assertThat(fooApi.put(new Foo("1", "foo"))).isEqualTo(new Foo("1", "foo"));
        assertThat(fooApi.delete(new Foo("1", "foo"))).isEqualTo(new Foo("1", "foo"));

        assertThat(fooApi.complex(new Foo("1", "foo1"), new Foo("2", "foo2")))
                .isEqualTo(List.of(new Foo("1", "foo1"), new Foo("2", "foo2")));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> fooApi.findAll(Map.of()))
                .withMessageContaining("No suitable resolver");

        // known issue, empty bean will not be resolved by BeanToQueryArgumentResolver,
        // don't plan to support this case.
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> fooApi.findAll(new EmptyBean()))
                .withMessageContaining("No suitable resolver");

        ctx.close();
    }

    record Foo(String id, String name) {}

    record EmptyBean() {}

    interface FooApi {
        @GetExchange("/foo")
        List<Foo> findAll(Foo foo);

        @PostExchange("/foo")
        Foo post(Foo foo);

        @PutExchange("/foo")
        Foo put(Foo foo);

        @DeleteExchange("/foo")
        Foo delete(Foo foo);

        @PostExchange("/foo/complex")
        List<Foo> complex(Foo foo, @RequestBody Foo foo2);

        @GetExchange("/foo/by-map")
        List<Foo> findAll(Map<String, Object> map);

        @GetExchange("/foo/emtpy-bean")
        default List<Foo> findAll(EmptyBean emptyBean) {
            return List.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    static class FooController implements FooApi {

        @Override
        @GetMapping("/foo")
        public List<Foo> findAll(Foo foo) {
            return List.of(foo);
        }

        @Override
        @PostMapping("/foo")
        public Foo post(Foo foo) {
            return foo;
        }

        @Override
        @PutMapping("/foo")
        public Foo put(Foo foo) {
            return foo;
        }

        @Override
        @DeleteMapping("/foo")
        public Foo delete(Foo foo) {
            return foo;
        }

        @PostMapping("/foo/complex")
        public List<Foo> complex(Foo foo, Foo foo2) {
            return List.of(foo, foo2);
        }

        @Override
        @GetMapping("/foo/by-map")
        public List<Foo> findAll(Map<String, Object> map) {
            return List.of(new Foo("1", "dummy"));
        }
    }
}
