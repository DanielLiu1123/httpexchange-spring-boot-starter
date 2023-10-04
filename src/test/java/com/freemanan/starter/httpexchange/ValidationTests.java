package com.freemanan.starter.httpexchange;

import static com.freemanan.cr.core.anno.Verb.ADD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.starter.PortFinder;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class ValidationTests {

    @Test
    void worksFine_whenSpringBootGreater3_0_3() {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(ValidateController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        ValidateApi api = ctx.getBean(ValidateApi.class);

        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> api.validate(0));
        assertThatCode(() -> api.validate(1)).doesNotThrowAnyException();
        assertThatCode(() -> api.validate(2)).doesNotThrowAnyException();
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> api.validate(3));

        ctx.close();
    }

    @Test
    @ClasspathReplacer({
        @Action(verb = ADD, value = "org.springframework.boot:spring-boot-starter-webflux:3.0.2"),
        @Action(verb = ADD, value = "org.springframework.boot:spring-boot-starter-validation:3.0.2")
    })
    void notWork_whenSpringBootLessThan3_0_3(CapturedOutput output) {
        int port = PortFinder.availablePort();
        var ctx = new SpringApplicationBuilder(ValidateController.class)
                .properties("server.port=" + port)
                .properties(HttpClientsProperties.PREFIX + ".base-url=localhost:" + port)
                .run();
        ValidateApi api = ctx.getBean(ValidateApi.class);

        assertThatCode(() -> api.validate(1)).doesNotThrowAnyException();
        assertThatCode(() -> api.validate(2)).doesNotThrowAnyException();
        // should throw ConstraintViolationException if validation works
        assertThatCode(() -> api.validate(3)).isInstanceOf(WebClientResponseException.class);
        assertThat(output).contains("ConstraintViolationException", "validate.id: must be less than or equal to 2");

        ctx.close();
    }

    @Validated
    interface ValidateApi {

        @GetExchange("/validate/{id}")
        String validate(@PathVariable @Min(1) @Max(2) int id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = ValidateApi.class)
    @RestController
    static class ValidateController implements ValidateApi {
        @Override
        @GetMapping("/validate/{id}")
        public String validate(/*@PathVariable @Min(1) @Max(2)(not necessary)*/ int id) {
            return "validated: " + id;
        }
    }
}
