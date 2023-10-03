package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.Dependencies.springBootVersion;
import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.PortGetter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class DynamicRefreshTests {

    @Test
    @ClasspathReplacer({@Action("org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion)})
    void testDynamicRefresh() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("http-exchange.base-url=http://localhost:" + port)
                .properties("http-exchange.refresh.enabled=true")
                .run();

        // Three beans: controller bean, api bean, proxied api bean
        assertThat(ctx.getBeanProvider(FooApi.class)).hasSize(3);

        FooApi api = ctx.getBean(FooApi.class);

        assertThat(api.get()).isEqualTo("OK");

        System.setProperty("http-exchange.base-url", "http://localhost:" + port + "/v2");
        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        // base-url changed
        assertThat(api.get()).isEqualTo("OK v2");

        System.clearProperty("http-exchange.base-url");
        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    static class Cfg implements FooApi {

        @Override
        @GetMapping("/get")
        public String get() {
            return "OK";
        }

        @GetMapping("/v2/get")
        public String getV2() {
            return "OK v2";
        }
    }

    interface FooApi {

        @GetExchange("/get")
        String get();
    }
}
