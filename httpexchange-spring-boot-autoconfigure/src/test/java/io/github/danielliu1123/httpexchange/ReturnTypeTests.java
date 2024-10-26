package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.PortGetter.availablePort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
class ReturnTypeTests {

    @Test
    void testReturnType() {
        int port = availablePort();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .properties("http-exchange.base-url=localhost:" + port)
                .run()) {

            Api api = ctx.getBean(Api.class);

            assertThatCode(api::get).doesNotThrowAnyException();
            assertThatCode(() -> Requester.create().addHeader("error", "true").call(api::get))
                    .isInstanceOf(
                            HttpClientErrorException.BadRequest
                                    .class); // return void will throw exception, not like Spring Cloud OpenFeign
            assertThat(api.getBody()).containsEntry("name", "Freeman");
            assertThat(api.getHeaders()).containsEntry("foo", List.of("bar"));
            assertThat(api.getResponseEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    interface Api {
        @GetExchange("/get")
        ResponseEntity<Map<String, Object>> getResponseEntity();

        @GetExchange("/get")
        Map<String, Object> getBody();

        @GetExchange("/get")
        HttpHeaders getHeaders();

        @GetExchange("/get")
        void get();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableExchangeClients
    @RestController
    static class Cfg {

        @GetMapping("/get")
        public ResponseEntity<?> get(@RequestHeader(value = "error", defaultValue = "false") boolean isError) {
            if (isError) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter error");
            }
            return ResponseEntity.ok()
                    .headers(headers -> headers.put("foo", List.of("bar")))
                    .body(Map.of("name", "Freeman"));
        }
    }
}
