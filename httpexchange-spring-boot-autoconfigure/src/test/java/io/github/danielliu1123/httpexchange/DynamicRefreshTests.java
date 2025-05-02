package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Size;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class DynamicRefreshTests {

    @AfterEach
    void reset() {
        System.clearProperty("http-exchange.base-url");
    }

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT"})
    void testDynamicRefresh(String clientType) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("http-exchange.base-url=http://localhost:" + port)
                .properties("http-exchange.client-type=" + clientType)
                .properties("http-exchange.refresh.enabled=true")
                .run()) {

            // Two beans: api bean, proxied api bean
            assertThat(ctx.getBeanProvider(FooApi.class)).hasSize(2);
            assertThat(ctx.getBeanProvider(BarApi.class)).hasSize(2);
            assertThat(ctx.getBeanProvider(BazApi.class)).hasSize(2);

            FooApi fooApi = ctx.getBean(FooApi.class);
            BarApi barApi = ctx.getBean(BarApi.class);
            BazApi bazApi = ctx.getBean(BazApi.class);

            assertThat(fooApi.get()).isEqualTo("OK");
            assertThat(barApi.get()).isEqualTo("OK");
            assertThat(bazApi.get("aaaaa")).isEqualTo("OK");
            assertThatCode(() -> bazApi.get("aaaaaa"))
                    .isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("size must be between 0 and 5");

            System.setProperty("http-exchange.base-url", "http://localhost:" + port + "/v2");
            ctx.publishEvent(new RefreshEvent(ctx, null, null));

            // base-url changed
            assertThat(fooApi.get()).isEqualTo("OK v2");
            assertThat(barApi.get()).isEqualTo("OK v2");
            assertThat(bazApi.get("aaaaa")).isEqualTo("OK v2");
            assertThatCode(() -> bazApi.get("aaaaaa"))
                    .isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("size must be between 0 and 5");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = {FooApi.class, BarApi.class, BazApi.class})
    @RestController
    static class Cfg {

        @GetMapping("/get")
        @SneakyThrows
        public String get() {
            Thread.sleep(10);
            return "OK";
        }

        @GetMapping("/v2/get")
        @SneakyThrows
        public String getV2() {
            Thread.sleep(10);
            return "OK v2";
        }
    }

    interface FooApi {

        @GetExchange("/get")
        String get();
    }

    interface BarApi {

        @GetExchange("/get")
        String get();
    }

    @Validated
    interface BazApi {

        @GetExchange("/get")
        String get(@RequestParam @Size(max = 5) String str);
    }
}
