package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielliu1123.PortGetter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

@ExtendWith(OutputCaptureExtension.class)
class HttpClientCustomizerIT {
    private static final Logger log = LoggerFactory.getLogger(HttpClientCustomizerIT.class);

    @ParameterizedTest
    @ValueSource(strings = {"REST_CLIENT", "REST_TEMPLATE", "WEB_CLIENT"})
    void testRestClient(String clientType, CapturedOutput output) {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(RestClientCfg.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=" + port)
                .properties("http-exchange.client-type=" + clientType)
                .properties("http-exchange.base-url=localhost:" + port)
                .run();

        ctx.getBean(Api.class).get();

        assertThat(output).contains("Customizing the " + clientType + "...");

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = Api.class)
    @RestController
    static class RestClientCfg implements Api {
        @Bean
        HttpClientCustomizer.RestClientCustomizer restClientCustomizer() {
            return (client, channel) -> client.requestInterceptor((request, body, execution) -> {
                log.info("Customizing the REST_CLIENT...");
                return execution.execute(request, body);
            });
        }

        @Bean
        HttpClientCustomizer.RestTemplateCustomizer restTemplateCustomizer() {
            return (restTemplate, channel) -> restTemplate.getInterceptors().add((request, body, execution) -> {
                log.info("Customizing the REST_TEMPLATE...");
                return execution.execute(request, body);
            });
        }

        @Bean
        HttpClientCustomizer.WebClientCustomizer webClientCustomizer() {
            return (webClient, channel) -> webClient.filter((request, next) -> {
                log.info("Customizing the WEB_CLIENT...");
                return next.exchange(request);
            });
        }

        @Override
        public String get() {
            return "OK";
        }
    }

    interface Api {
        @GetExchange("/get")
        String get();
    }
}
