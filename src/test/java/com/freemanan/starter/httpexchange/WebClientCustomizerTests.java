package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.starter.PortGetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class WebClientCustomizerTests {

    @Test
    void testAddInterceptor(CapturedOutput output) {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();

        String resp = ctx.getBean(FooApi.class).get();

        assertThat(resp).isEqualTo("Hello World!");
        assertThat(output).contains("Response status: 200 OK");

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    @RestController
    static class Cfg implements FooApi {
        private static final Logger log = LoggerFactory.getLogger(Cfg.class);

        @Bean
        WebClientCustomizer loggingCustomizer() {
            return builder -> builder.filter((request, next) -> {
                // log response here
                return next.exchange(request)
                        .doOnNext(response -> log.info("Response status: {}", response.statusCode()));
            });
        }

        @Override
        public String get() {
            return "Hello World!";
        }
    }

    @RequestMapping("/foo")
    interface FooApi {

        @GetMapping
        String get();
    }
}
