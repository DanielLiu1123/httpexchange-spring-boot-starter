package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.order.api.OrderApi;
import com.freemanan.starter.user.api.DummyApi;
import com.freemanan.starter.user.api.UserApi;
import com.freemanan.starter.user.api.UserHobbyApi;
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
                "com.freemanan",
                "com.freemanan.starter.**.api",
                "**.api",
            })
    void testBasePackages(String pkg) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .web(WebApplicationType.NONE)
                .properties(HttpClientsProperties.PREFIX + ".base-packages=" + pkg)
                .run();

        assertThatCode(() -> ctx.getBean(HttpClientBeanDefinitionRegistry.class))
                .doesNotThrowAnyException();

        assertThatCode(() -> ctx.getBean(OrderApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(UserApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(UserHobbyApi.class)).doesNotThrowAnyException();
        assertThatCode(() -> ctx.getBean(DummyApi.class)).isInstanceOf(NoSuchBeanDefinitionException.class);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
