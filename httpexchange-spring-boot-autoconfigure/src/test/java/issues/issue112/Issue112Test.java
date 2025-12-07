package issues.issue112;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
@SpringBootTest
@Disabled("Just a showcase")
class Issue112Test {

    interface Api {
        @GetExchange("/typicode/demo/posts")
        List<Post> batchGetPosts();
    }

    record Post(Integer id, String title) {}

    @Autowired
    Api api;

    @BeforeEach
    void setup() {
        // Mock your server, got a dynamic url
        var yourDynamicUrl = "https://my-json-server.typicode.com/typicode/demo/posts";
        System.setProperty("dynamicUrl", yourDynamicUrl);
    }

    @AfterEach
    void clear() {
        System.clearProperty("dynamicUrl");
    }

    @Test
    void testBatchGetPosts() {
        var posts = api.batchGetPosts();

        assertThat(posts).hasSize(3);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = Api.class)
    static class Cfg {
        @Bean
        RestClientCustomizer dynamicUrlCustomizer() {
            return builder -> builder.requestInterceptor((request, body, execution) -> {
                var dynamicUrl = System.getProperty("dynamicUrl");
                if (dynamicUrl != null) {
                    request = new HttpRequestWrapper(request) {
                        @Override
                        public URI getURI() {
                            return URI.create(dynamicUrl);
                        }
                    };
                }
                return execution.execute(request, body);
            });
        }
    }
}
