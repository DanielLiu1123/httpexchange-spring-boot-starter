package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import io.github.danielliu1123.Post;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
class UrlVariableTests {
    @Test
    void testUrlVariable() {
        int port = findAvailableTcpPort();
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlVariableController.class)
                .properties("server.port=" + port)
                .run("--api.url=http://localhost:" + port)) {

            assertThatCode(() -> ctx.getBean(UrlVariableApi.class)).doesNotThrowAnyException();

            UrlVariableApi api = ctx.getBean(UrlVariableApi.class);
            List<Post> posts = api.getPosts();

            assertThat(posts).isEmpty();
        }
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
    @HttpExchange
    static class UrlVariableController implements UrlVariableApi {

        @Override
        @GetExchange("/posts")
        public List<Post> getPosts() {
            return List.of();
        }
    }
}
