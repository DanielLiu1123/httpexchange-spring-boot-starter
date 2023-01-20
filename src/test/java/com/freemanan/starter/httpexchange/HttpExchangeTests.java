package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.starter.order.api.OrderApi;
import com.freemanan.starter.user.api.UserApi;
import com.freemanan.starter.user.api.UserHobbyApi;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

class HttpExchangeTests {

    @Test
    void testNotEnableHttpExchange() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(NotEnableHttpExchange.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(UserApi.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(PostApi.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> ctx.getBean(UserHobbyApi.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(OrderApi.class));

        ctx.close();
    }

    @Test
    void testDefaultConfig() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(DefaultConfig.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(PostApi.class)).doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(UserApi.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(OrderApi.class));

        ctx.close();
    }

    @Test
    void testBasePackage_whenParentPackage() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ParentPackage.class)
                .web(WebApplicationType.NONE)
                .properties("logging.level.com.freemanan.starter=DEBUG")
                .properties("logging.level.org.springframework.web=DEBUG")
                .run();

        PostApi postApi = ctx.getBean(PostApi.class);
        OrderApi orderApi = ctx.getBean(OrderApi.class);
        UserApi userApi = ctx.getBean(UserApi.class);
        UserHobbyApi userHobbyApi = ctx.getBean(UserHobbyApi.class);

        assertThat(postApi.getPosts()).isNotEmpty();
        assertThat(orderApi).isNotNull();
        assertThat(userApi).isNotNull();
        assertThat(userHobbyApi).isNotNull();

        ResponseEntity<Map<String, String>> response = userHobbyApi.getUserHobby("1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        ctx.close();
    }

    @Test
    void testBasePackage_whenWildcard() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Wildcard.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(OrderApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(PostApi.class));

        ctx.close();
    }

    @Test
    void testBasePackage_whenSpecificPackage() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SpecificPackage.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(OrderApi.class)).doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(UserApi.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(PostApi.class));

        ctx.close();
    }

    @Test
    void testBasePackage_whenSpecificPackageAndClients_thenBasePackageShouldNotWorking() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SpecificPackageAndClients.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(OrderApi.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(PostApi.class));

        ctx.close();
    }

    @Test
    void testClientsProperty() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ClientsProperty.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(PostApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(OrderApi.class));

        ctx.close();
    }

    @Test
    void testDefaultMethodBehavior() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ParentPackage.class)
                .web(WebApplicationType.NONE)
                .run();

        PostApi postApi = ctx.getBean(PostApi.class);

        assertThatExceptionOfType(UndeclaredThrowableException.class)
                .isThrownBy(() -> postApi.getPost())
                .withRootCauseInstanceOf(IllegalAccessException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class NotEnableHttpExchange {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableHttpExchanges
    static class DefaultConfig {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableHttpExchanges({
        "com.freemanan.starter",
    })
    static class ParentPackage {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableHttpExchanges({"com.freemanan.**.api"})
    static class Wildcard {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableHttpExchanges({"com.freemanan.starter.order.api"})
    static class SpecificPackage {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableHttpExchanges(
            value = {"com.freemanan.starter.order.api"},
            clients = UserApi.class)
    static class SpecificPackageAndClients {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableHttpExchanges(clients = {UserApi.class, PostApi.class})
    static class ClientsProperty {}

    @HttpExchange("https://my-json-server.typicode.com/")
    interface PostApi {

        @GetExchange("/typicode/demo/posts")
        List<Post> getPosts();

        default String getPost() {
            return "post";
        }
    }

    static class Post {
        private Integer id;
        private String title;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "Post{" + "id=" + id + ", title='" + title + '\'' + '}';
        }
    }
}
