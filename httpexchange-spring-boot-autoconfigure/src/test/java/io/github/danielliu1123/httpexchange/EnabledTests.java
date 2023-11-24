package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.danielliu1123.Post;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class EnabledTests {

    @Test
    void testDisabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(EnabledConfig.class)
                .web(WebApplicationType.NONE)
                .properties(HttpExchangeProperties.PREFIX + ".enabled=false")
                .run();

        assertThatCode(() -> ctx.getBean(EnabledApi.class)).isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Test
    void testEnabled() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(EnabledConfig.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(EnabledApi.class)).doesNotThrowAnyException();

        ctx.close();
    }

    interface EnabledApi {
        @GetExchange("/posts")
        List<Post> getPosts();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = EnabledApi.class)
    static class EnabledConfig {}
}
