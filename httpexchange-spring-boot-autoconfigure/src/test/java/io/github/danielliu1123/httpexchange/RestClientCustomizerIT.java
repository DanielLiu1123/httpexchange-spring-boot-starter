package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielliu1123.PortGetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class RestClientCustomizerIT {

    @Test
    void testAddInterceptor(CapturedOutput output) {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
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
        RestClientCustomizer loggingCustomizer2() {
            return builder -> builder.requestInterceptor((request, body, execution) -> {
                ClientHttpResponse response = execution.execute(request, body);
                log.info("Response status: {}", response.getStatusCode());
                return response;
            });
        }

        @Override
        public String get() {
            return "Hello World!";
        }
    }

    @HttpExchange("/foo")
    interface FooApi {

        @GetExchange
        String get();
    }
}
