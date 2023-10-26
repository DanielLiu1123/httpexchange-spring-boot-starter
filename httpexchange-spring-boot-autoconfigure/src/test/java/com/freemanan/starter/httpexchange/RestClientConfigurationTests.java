package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.PortGetter;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class RestClientConfigurationTests {

    @Test
    void testRestClientCustomizer(CapturedOutput output) {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(Controller.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        Api api = ctx.getBean(Api.class);

        assertThatCode(api::get).doesNotThrowAnyException();
        assertThat(output).contains("Intercepted!");

        ctx.close();
    }

    interface Api {
        @GetExchange("/get")
        String get();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = Api.class)
    @RestController
    static class Controller implements Api {
        private static final Logger log = LoggerFactory.getLogger(Controller.class);

        @Override
        public String get() {
            return "OK";
        }

        @Bean
        RestClientCustomizer restClientCustomizer() {
            return builder -> builder.requestInterceptor((request, body, execution) -> {
                log.info("Intercepted!");
                return execution.execute(request, body);
            });
        }
    }
}
