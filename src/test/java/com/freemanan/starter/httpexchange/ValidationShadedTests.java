package com.freemanan.starter.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.starter.PortFinder;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class ValidationShadedTests {

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

    @Validated
    interface ValidateApi {

        @GetMapping("/validate/{id}")
        String validate(@PathVariable @Min(1) @Max(2) int id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients(clients = ValidateApi.class)
    @RestController
    static class ValidateController implements ValidateApi {
        @Override
        public String validate(/*@PathVariable @Min(1) @Max(2)(not necessary)*/ int id) {
            return "validated: " + id;
        }
    }
}
