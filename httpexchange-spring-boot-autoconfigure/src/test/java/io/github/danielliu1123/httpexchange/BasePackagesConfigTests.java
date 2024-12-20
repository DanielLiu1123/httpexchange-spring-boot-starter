package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.danielliu1123.order.api.OrderApi;
import io.github.danielliu1123.user.api.DummyApi;
import io.github.danielliu1123.user.api.UserApi;
import io.github.danielliu1123.user.api.UserHobbyApi;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
class BasePackagesConfigTests {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "io.github",
                "io.github.danielliu1123.**.api",
                "**.api",
            })
    void testBasePackages(String pkg) {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .web(WebApplicationType.NONE)
                .properties(HttpExchangeProperties.PREFIX + ".base-packages=" + pkg)
                .run()) {

            assertThatCode(() -> ctx.getBean(HttpClientBeanDefinitionRegistry.class))
                    .doesNotThrowAnyException();

            assertThatCode(() -> ctx.getBean(OrderApi.class)).doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(UserHobbyApi.class)).doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(DummyApi.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
