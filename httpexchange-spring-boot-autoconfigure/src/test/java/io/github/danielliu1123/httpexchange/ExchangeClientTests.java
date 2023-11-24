package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.danielliu1123.Post;
import io.github.danielliu1123.order.api.OrderApi;
import io.github.danielliu1123.user.api.UserApi;
import io.github.danielliu1123.user.api.UserHobbyApi;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Core tests.
 *
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class ExchangeClientTests {

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
                .properties("logging.level.io.github.danielliu1123=DEBUG")
                .properties("logging.level.org.springframework.web=DEBUG")
                .run();

        assertThatCode(() -> ctx.getBean(PostApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(OrderApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(UserHobbyApi.class)).doesNotThrowAnyException();

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
    void testBasePackage_whenSpecificPackageAndClients_thenBasePackageShouldWork(CapturedOutput output) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SpecificPackageAndClients.class)
                .web(WebApplicationType.NONE)
                .run();

        assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(OrderApi.class)).doesNotThrowAnyException();

        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> ctx.getBean(PostApi.class));

        assertThat(output).contains("you can remove it from 'clients' property.");

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

        assertThatCode(postApi::getDeletedPosts).isInstanceOf(UndeclaredThrowableException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class NotEnableHttpExchange {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    static class DefaultConfig {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients({
        "io.github.danielliu1123",
    })
    static class ParentPackage {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients({"io.github.danielliu1123.**.api"})
    static class Wildcard {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients({"io.github.danielliu1123.order.api"})
    static class SpecificPackage {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(
            value = {"io.github.danielliu1123.order.api"},
            clients = {UserApi.class, OrderApi.class})
    static class SpecificPackageAndClients {}

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = {UserApi.class, PostApi.class})
    static class ClientsProperty {}

    interface PostApi {

        @GetExchange("/posts")
        List<Post> getPosts();

        @GetExchange("/posts/search")
        List<Post> getPosts(Post post);

        @GetExchange("/posts/{id}")
        Post getPost(@PathVariable int id);

        default List<Post> getDeletedPosts() {
            throw new UnsupportedOperationException();
        }
    }
}
