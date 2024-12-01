package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class RegisterBeanManuallyTests {

    static int port = findAvailableTcpPort();

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBean() {
        try (var ctx = new SpringApplicationBuilder(CfgWithoutApiCfg.class)
                .web(WebApplicationType.SERVLET)
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
        try (var ctx = new SpringApplicationBuilder(CfgWithApiCfg.class)
                .web(WebApplicationType.SERVLET)
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
    @Import(ApiCfg.class)
    @EnableExchangeClients(clients = Api.class)
    @RestController
    static class CfgWithApiCfg {

        @GetExchange("/{id}")
        public String get(@PathVariable long id) {
            return "Hello " + id;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = Api.class)
    @RestController
    static class CfgWithoutApiCfg {

        @GetExchange("/{id}")
        public String get(@PathVariable long id) {
            return "Hello " + id;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ApiCfg {
        @Bean
        public Api api(RestClient.Builder builder) {
            builder.baseUrl("http://localhost:" + port);
            return HttpServiceProxyFactory.builder()
                    .exchangeAdapter(RestClientAdapter.create(builder.build()))
                    .build()
                    .createClient(Api.class);
        }
    }

    interface Api {
        @GetExchange("/{id}")
        String get(@PathVariable long id);
    }
}
