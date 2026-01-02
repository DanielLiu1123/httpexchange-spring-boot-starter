package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class MultipleMatchedChannelsIT {

    @Test
    void testMultipleMatchedChannels_whenMatchingViaClasses(CapturedOutput output) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .web(WebApplicationType.NONE)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].name=channel1")
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].base-url=http://localhost:${server.port}")
                .properties(
                        HttpExchangeProperties.PREFIX
                                + ".channels[0].classes[0]=io.github.danielliu1123.httpexchange.MultipleMatchedChannelsIT$FooApi")
                .properties(HttpExchangeProperties.PREFIX + ".channels[1].name=channel2")
                .properties(HttpExchangeProperties.PREFIX + ".channels[1].base-url=http://localhost:${server.port}")
                .properties(
                        HttpExchangeProperties.PREFIX
                                + ".channels[1].classes[0]=io.github.danielliu1123.httpexchange.MultipleMatchedChannelsIT$FooApi")
                .run()) {

            assertThatCode(() -> ctx.getBean(FooApi.class)).doesNotThrowAnyException();
            assertThat(output)
                    .contains(
                            "Exchange client [io.github.danielliu1123.httpexchange.MultipleMatchedChannelsIT$FooApi] matched multiple channels: [channel1, channel2], using 'channel1'");
        }
    }

    @Test
    void testMultipleMatchedChannels_whenMatchingViaClients(CapturedOutput output) {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .web(WebApplicationType.NONE)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].name=channel1")
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].base-url=http://localhost:${server.port}")
                .properties(HttpExchangeProperties.PREFIX + ".channels[0].clients[0]=FooApi")
                .properties(HttpExchangeProperties.PREFIX + ".channels[1].name=channel2")
                .properties(HttpExchangeProperties.PREFIX + ".channels[1].base-url=http://localhost:${server.port}")
                .properties(HttpExchangeProperties.PREFIX
                        + ".channels[1].clients[0]=io.github.danielliu1123.httpexchange.**")
                .run()) {

            assertThatCode(() -> ctx.getBean(FooApi.class)).doesNotThrowAnyException();
            assertThat(output)
                    .contains(
                            "Exchange client [io.github.danielliu1123.httpexchange.MultipleMatchedChannelsIT$FooApi] matched multiple channels: [channel1, channel2], using 'channel1'");
        }
    }

    @HttpExchange("/foo")
    public interface FooApi {
        @GetExchange
        String get();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = FooApi.class)
    static class Cfg {}
}
