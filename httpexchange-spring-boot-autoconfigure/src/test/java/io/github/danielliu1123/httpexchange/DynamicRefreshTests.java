package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.PortGetter.availablePort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class DynamicRefreshTests {

    @AfterEach
    void reset() {
        System.clearProperty("http-exchange.base-url");
    }

    @Test
    void testDynamicRefresh() {
        int port = availablePort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("http-exchange.base-url=http://localhost:" + port)
                .properties("http-exchange.refresh.enabled=true")
                .run();

        // Three beans: controller bean, api bean, proxied api bean
        assertThat(ctx.getBeanProvider(FooApi.class)).hasSize(3);
        assertThat(ctx.getBeanProvider(BarApi.class)).hasSize(2);

        FooApi api = ctx.getBean(FooApi.class);
        BarApi barApi = ctx.getBean(BarApi.class);

        assertThat(api.get()).isEqualTo("OK");
        assertThat(barApi.get()).isEqualTo("OK");
        assertThat(barApi.withTimeout(1000).get()).isEqualTo("OK");
        assertThatCode(() -> barApi.withTimeout(1000).withTimeout(5).get())
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("request timed out");

        System.setProperty("http-exchange.base-url", "http://localhost:" + port + "/v2");
        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        // base-url changed
        assertThat(api.get()).isEqualTo("OK v2");
        assertThat(barApi.get()).isEqualTo("OK v2");
        assertThat(barApi.withTimeout(1000).get()).isEqualTo("OK v2");
        assertThatCode(() -> barApi.withTimeout(5).get())
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("request timed out");

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = {FooApi.class, BarApi.class})
    @RestController
    static class Cfg implements FooApi {

        @Override
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

    interface BarApi extends RequestConfigurator<BarApi> {

        @GetExchange("/get")
        String get();
    }
}
