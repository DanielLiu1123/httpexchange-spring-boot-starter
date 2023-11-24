package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.danielliu1123.PortGetter;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * {@link BeanParamArgumentResolver} tester.
 *
 * @author Freeman
 */
class BeanParamArgumentResolverTests {

    @Test
    void convertObjectPropertiesToRequestParameters() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=http://localhost:" + port)
                .properties(HttpExchangeProperties.PREFIX + ".bean-to-query-enabled=true")
                .run();

        FooApi fooApi = ctx.getBean(FooApi.class);

        assertThat(fooApi).isNotInstanceOf(FooController.class);

        assertThat(fooApi.findAll(new Foo("1", "foo1"))).isEqualTo(List.of(new Foo("1", "foo1")));
        assertThat(fooApi.post(new Foo("1", "foo"))).isEqualTo(new Foo("1", "foo"));
        assertThat(fooApi.put(new Foo("1", "foo"))).isEqualTo(new Foo("1", "foo"));
        assertThat(fooApi.delete(new Foo("1", "foo"))).isEqualTo(new Foo("1", "foo"));

        assertThat(fooApi.complex(new Foo("1", "foo1"), new Foo("2", "foo2")))
                .isEqualTo(List.of(new Foo("1", "foo1"), new Foo("2", "foo2")));

        // test @QueryMap
        assertThat(fooApi.testBeanParam(new Foo("1", "foo1"))).isEqualTo(new Foo("1", "foo1"));

        // test @RequestParam for Map
        assertThat(fooApi.testRequestParamForMap(Map.of("id", "1", "name", "foo1")))
                .isEqualTo(Map.of("id", "1", "name", "foo1"));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> fooApi.findAll(Map.of()))
                .withMessageContaining("No suitable resolver");

        assertThatCode(() -> fooApi.findAll(new EmptyBean())).doesNotThrowAnyException();

        Date date = new Date();
        FooWithArrProp resp = fooApi.testArrProp(new FooWithArrProp(
                "1", new String[] {"a", "b"}, List.of(1, 2), date, URI.create("http://localhost:8080")));
        assertThat(resp.id()).isEqualTo("1");
        assertThat(resp.arr()).isEqualTo(new String[] {"a", "b"});
        assertThat(resp.list()).isEqualTo(List.of(1, 2));
        assertThat(resp.date()).isNotNull();
        assertThat(resp.date()).isNotEqualTo(date); // FIXME(Freeman): known issue, loss milliseconds
        assertThat(resp.url()).isEqualTo(URI.create("http://localhost:8080"));

        ctx.close();
    }

    @Test
    void hasQueryMapArgumentResolverBean_whenDefaultConfig() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(FooController.class)
                .web(WebApplicationType.NONE)
                .properties("server.port=" + port)
                .run();

        assertThatCode(() -> ctx.getBean(BeanParamArgumentResolver.class)).doesNotThrowAnyException();

        ctx.close();
    }

    record Foo(String id, String name) {}

    record FooWithArrProp(String id, String[] arr, List<Integer> list, Date date, URI url) {}

    record EmptyBean() {}

    interface FooApi {
        @GetExchange("/foo")
        List<Foo> findAll(Foo foo);

        @GetExchange("/FooWithArrProp")
        FooWithArrProp testArrProp(FooWithArrProp foo);

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

        @GetExchange("/foo/testBeanParam")
        Foo testBeanParam(@BeanParam Foo foo);

        @GetExchange("/foo/testRequestParamForMap")
        Map<String, String> testRequestParamForMap(@RequestParam Map<String, String> map);
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

        @Override
        public Foo testBeanParam(Foo foo) {
            return foo;
        }

        @Override
        public Map<String, String> testRequestParamForMap(Map<String, String> map) {
            return map;
        }
    }
}
