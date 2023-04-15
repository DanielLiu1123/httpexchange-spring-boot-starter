package com.freemanan.starter.httpexchange;

import static com.freemanan.starter.Dependencies.springBootVersion;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.cr.core.anno.Verb;
import com.freemanan.starter.PortFinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
@ClasspathReplacer(
        @Action(verb = Verb.ADD, value = "org.springframework.boot:spring-boot-starter-webflux:" + springBootVersion))
public class TimeoutTests {

    @Test
    void testDefaultTimeout_whenExceed() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".response-timeout=200")
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> api.delay(300))
                .withMessageContaining("Timeout on blocking read for 200000000 NANOSECONDS");

        ctx.close();
    }

    @Test
    void testDefaultTimeout_whenNotExceed() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".response-timeout=200")
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(100)).doesNotThrowAnyException();

        ctx.close();
    }

    @Test
    void testTimeout_whenExceed() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(TimeoutConfig.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".response-timeout=200")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].name=DelayApi")
                .properties(HttpClientsProperties.PREFIX + ".clients[0].base-url=http://localhost:" + port)
                .properties(HttpClientsProperties.PREFIX + ".clients[0].response-timeout=600")
                .run();
        DelayApi api = ctx.getBean(DelayApi.class);

        assertThatCode(() -> api.delay(400)).doesNotThrowAnyException();

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
    static class TimeoutConfig implements DelayApi {

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