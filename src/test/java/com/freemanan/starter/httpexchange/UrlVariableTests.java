package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.PortFinder;
import com.freemanan.starter.Post;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class UrlVariableTests {
    @Test
    void testUrlVariable() {
        int port = PortFinder.availablePort();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlVariableController.class)
                .properties("server.port=" + port)
                .run("--api.url=http://localhost:" + port);

        assertThatCode(() -> ctx.getBean(UrlVariableApi.class)).doesNotThrowAnyException();

        UrlVariableApi api = ctx.getBean(UrlVariableApi.class);
        List<Post> posts = api.getPosts();

        assertThat(posts).isEmpty();

        ctx.close();
    }

    @HttpExchange("${api.url}")
    interface UrlVariableApi {
        @GetExchange("/posts")
        List<Post> getPosts();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = UrlVariableApi.class)
    @RestController
    static class UrlVariableController implements UrlVariableApi {

        @Override
        @GetMapping("/posts")
        public List<Post> getPosts() {
            return List.of();
        }
    }
}
