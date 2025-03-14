package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class ValidationTests {

    @Test
    void worksFine_whenSpringBootGreater3_0_3() {
        int port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(ValidateController.class)
                .properties("server.port=" + port)
                .properties(HttpExchangeProperties.PREFIX + ".base-url=localhost:" + port)
                .run()) {
            ValidateApi api = ctx.getBean(ValidateApi.class);

            assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> api.validate(0));
            assertThatCode(() -> api.validate(1)).doesNotThrowAnyException();
            assertThatCode(() -> api.validate(2)).doesNotThrowAnyException();
            assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> api.validate(3));
        }
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
        public String validate(int id) {
            return "validated: " + id;
        }
    }
}
