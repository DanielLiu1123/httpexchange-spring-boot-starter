package com.freemanan.starter.httpexchange;

import static com.freemanan.cr.core.anno.Verb.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.Post;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
public class ValidationTests {

    @Test
    void worksFine_whenSpringBootGreater3_0_3() {
        var ctx = new SpringApplicationBuilder(ValidateConfig.class)
                .web(WebApplicationType.NONE)
                .run();
        ValidateApi api = ctx.getBean(ValidateApi.class);

        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> api.getPost(0));
        assertThatCode(() -> api.getPost(1)).doesNotThrowAnyException();
        assertThatCode(() -> api.getPost(2)).doesNotThrowAnyException();
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> api.getPost(3));
    }

    @Test
    @ClasspathReplacer({@Action(verb = OVERRIDE, value = "org.springframework.boot:spring-boot:3.0.2")})
    void notWork_whenSpringBootLessThan3_0_3() {
        var ctx = new SpringApplicationBuilder(ValidateConfig.class)
                .web(WebApplicationType.NONE)
                .run();
        ValidateApi api = ctx.getBean(ValidateApi.class);

        assertThatCode(() -> api.getPost(1)).doesNotThrowAnyException();
        assertThatCode(() -> api.getPost(2)).doesNotThrowAnyException();
        // should throw ConstraintViolationException if validation works
        assertThatCode(() -> api.getPost(3)).doesNotThrowAnyException();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = ValidateApi.class)
    static class ValidateConfig {}

    @HttpExchange("https://my-json-server.typicode.com/")
    @Validated
    interface ValidateApi {

        @GetExchange("/typicode/demo/posts/{id}")
        Post getPost(@PathVariable @Min(1) @Max(2) int id);
    }
}
