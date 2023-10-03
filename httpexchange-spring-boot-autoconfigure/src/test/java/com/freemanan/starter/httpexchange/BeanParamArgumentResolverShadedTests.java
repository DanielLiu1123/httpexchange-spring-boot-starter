package com.freemanan.starter.httpexchange;

import static com.freemanan.cr.core.anno.Verb.ADD;
import static com.freemanan.starter.Dependencies.springBootVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.PortFinder;
import java.net.URI;
import java.util.Date;
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

/**
 * {@link BeanParamArgumentResolver} tester.
 *
 * @author Freeman
 */
class BeanParamArgumentResolverShadedTests {

    @Test
    @ClasspathReplacer({
        @Action(verb = ADD, value = "org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion)
    })
    void convertObjectPropertiesToRequestParameters() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=http://localhost:" + port)
                .properties(HttpClientsProperties.PREFIX + ".bean-to-query=true")
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

        Date date = new Date();
        FooWithArrProp resp = fooApi.testArrProp(new FooWithArrProp(
                "1",
                new String[] {"a", "b"},
                List.of(1, 2),
                List.of(new Foo("1", "foo1")),
                date,
                URI.create("http://localhost:8080")));
        assertThat(resp.id()).isEqualTo("1");
        assertThat(resp.arr()).isEqualTo(new String[] {"a", "b"});
        assertThat(resp.list()).isEqualTo(List.of(1, 2));
        assertThat(resp.foos()).isNull();
        assertThat(resp.date()).isNotNull();
        assertThat(resp.date()).isNotEqualTo(date); // FIXME(Freeman): known issue, loss milliseconds
        assertThat(resp.url()).isEqualTo(URI.create("http://localhost:8080"));

        ctx.close();
    }

    record Foo(String id, String name) {}

    record FooWithArrProp(String id, String[] arr, List<Integer> list, List<Foo> foos, Date date, URI url) {}

    record EmptyBean() {}

    interface FooApi {
        @GetMapping("/foo")
        List<Foo> findAll(Foo foo);

        @GetMapping("/FooWithArrProp")
        FooWithArrProp testArrProp(FooWithArrProp foo);

        @PostMapping("/foo")
        Foo post(Foo foo);

        @PutMapping("/foo")
        Foo put(Foo foo);

        @DeleteMapping("/foo")
        Foo delete(Foo foo);

        @PostMapping("/foo/complex")
        List<Foo> complex(Foo foo, @RequestBody Foo foo2);

        @GetMapping("/foo/by-map")
        List<Foo> findAll(Map<String, Object> map);

        @GetMapping("/foo/emtpy-bean")
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
        public List<Foo> findAll(Foo foo) {
            return List.of(foo);
        }

        @Override
        public FooWithArrProp testArrProp(FooWithArrProp foo) {
            return foo;
        }

        @Override
        public Foo post(Foo foo) {
            return foo;
        }

        @Override
        public Foo put(Foo foo) {
            return foo;
        }

        @Override
        public Foo delete(Foo foo) {
            return foo;
        }

        @Override
        public List<Foo> complex(Foo foo, Foo foo2) {
            return List.of(foo, foo2);
        }

        @Override
        public List<Foo> findAll(Map<String, Object> map) {
            return List.of(new Foo("1", "dummy"));
        }
    }
}
