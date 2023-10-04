package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.PortGetter;
import io.netty.handler.timeout.ReadTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

/**
 * @author Freeman
 */
class WebClientConfigurationTests {

    @Test
    void testNotSetTimeout() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(1000)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testTimeoutExceed() {
        int port = PortGetter.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .properties("http.timeout=1")
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(1500)).hasRootCauseInstanceOf(ReadTimeoutException.class);

        ctx.close();
    }

    interface DelayApi {
        @GetExchange("/delay/{delay}")
        String delay(@PathVariable int delay);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = DelayApi.class)
    @RestController
    static class TimeoutController implements DelayApi {

        @Value("${http.timeout:0}")
        int timeout;

        @Bean
        WebClientCustomizer webClientCustomizer() {
            return builder -> {
                HttpClient httpClient = HttpClient.create();
                if (timeout > 0) {
                    httpClient = httpClient.responseTimeout(Duration.ofSeconds(timeout));
                }
                builder.clientConnector(new ReactorClientHttpConnector(httpClient));
            };
        }

        @Bean
        HttpServiceProxyFactory.Builder httpServiceProxyFactory(WebClient.Builder builder) {
            return HttpServiceProxyFactory.builder().clientAdapter(WebClientAdapter.forClient(builder.build()));
        }

        @Override
        @GetMapping("/delay/{delay}")
        public String delay(@PathVariable int delay) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "delayed " + delay + "ms";
        }
    }
}
