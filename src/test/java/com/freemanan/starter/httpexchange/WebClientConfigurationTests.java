package com.freemanan.starter.httpexchange;

import static com.freemanan.cr.core.anno.Verb.*;
import static com.freemanan.starter.Dependencies.*;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import io.netty.handler.timeout.ReadTimeoutException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

/**
 * @author Freeman
 */
@ClasspathReplacer({
    @Action(verb = ADD, value = "org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion)
})
public class WebClientConfigurationTests {

    @Test
    void testNotSetTimeout() {
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .web(WebApplicationType.NONE)
                .run();
        TimeoutApi api = ctx.getBean(TimeoutApi.class);

        assertThatCode(() -> api.delay(1)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testTimeoutExceed() {
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .web(WebApplicationType.NONE)
                .properties("http.timeout=1")
                .run();
        TimeoutApi api = ctx.getBean(TimeoutApi.class);

        assertThatCode(() -> api.delay(2)).hasRootCauseInstanceOf(ReadTimeoutException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = TimeoutApi.class)
    static class TimeoutConfig {

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
    }

    @HttpExchange("https://httpbin.org")
    @Validated
    interface TimeoutApi {

        @GetExchange("/delay/{delay}")
        void delay(@PathVariable @Min(0) @Max(10) int delay);
    }
}
