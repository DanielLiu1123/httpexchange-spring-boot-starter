package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.danielliu1123.PortGetter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class ManualRegisterBeanTests {

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBean() {
        var port = PortGetter.availablePort();

        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + (port - 1))
                .run()) {

            var api = ctx.getBean(Api.class);

            assertThatCode(() -> api.get(1))
                    .isInstanceOf(ResourceAccessException.class)
                    .hasMessageContaining("I/O error");
        }
    }

    @Test
    void useManualRegisteredBean_whenManualRegisteredBeanExists() {
        var port = PortGetter.availablePort();

        try (var ctx = new SpringApplicationBuilder(Cfg.class, ApiCfg.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + (port - 1))
                .run()) {

            var api = ctx.getBean(Api.class);

            var result = api.get(1);
            assertThat(result).isEqualTo("Hello 1");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = Api.class)
    @RestController
    static class Cfg {

        @GetExchange("/{id}")
        public String get(@PathVariable long id) {
            return "Hello " + id;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ApiCfg {
        @Bean
        public Api api(RestClient.Builder builder, WebServerApplicationContext ctx) {
            var factory = HttpServiceProxyFactory.builder()
                    .exchangeAdapter(RestClientAdapter.create(builder.baseUrl(
                                    "http://localhost:" + ctx.getWebServer().getPort())
                            .build()))
                    .build();
            return factory.createClient(Api.class);
        }
    }

    interface Api {
        @GetExchange("/{id}")
        String get(@PathVariable long id);
    }
}
