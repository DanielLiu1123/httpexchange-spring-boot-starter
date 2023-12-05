package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Spring Cloud load balancer is based on Spring Retry or Reactor.
 *
 * <p> If using sync clients, the retry depends on Spring Retry.
 * <p> If using async clients, the retry depends on Reactor.
 */
class LoadBalancerAppTests {

    /**
     * When using sync clients, the retry depends on Spring Retry.
     */
    @ParameterizedTest
    @ValueSource(strings = {"rest_client", "rest_template"})
    void testLoadBalancer_whenRetryDependsOnSpringRetry_thenAllRequestOK(String clientType) {
        int port = getRandomPort();

        var ctx = new SpringApplicationBuilder(LoadBalancerApp.class)
                .properties("server.port=" + port)
                .properties("http-exchange.client-type=" + clientType)
                .run();

        UserApi userApi = ctx.getBean(UserApi.class);

        int success = 0;
        int failure = 0;
        for (int i = 0; i < 4; i++) {
            try {
                userApi.getById("1");
                success++;
            } catch (Exception e) {
                failure++;
            }
        }

        assertThat(success).isEqualTo(4);
        assertThat(failure).isZero();

        ctx.close();
    }

    /**
     * When using async clients, the retry depends on Reactor.
     */
    @ParameterizedTest
    @ValueSource(strings = {"web_client"})
    void testLoadBalancer_whenRetryDependsOnReactor_thenAllRequestOK(String clientType) {
        int port = getRandomPort();

        var ctx = new SpringApplicationBuilder(LoadBalancerApp.class)
                .properties("server.port=" + port)
                .properties("http-exchange.client-type=" + clientType)
                .properties("spring.cloud.loadbalancer.retry.enabled=true")
                .run();

        UserApi userApi = ctx.getBean(UserApi.class);

        int success = 0;
        int failure = 0;
        for (int i = 0; i < 4; i++) {
            try {
                userApi.getById("1");
                success++;
            } catch (Exception e) {
                failure++;
            }
        }

        assertThat(success).isEqualTo(4);
        assertThat(failure).isZero();

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest_client", "rest_template", "web_client"})
    void testLoadBalancer_whenDisableRetry_thenHalfOKHalfFailed(String clientType) {
        int port = getRandomPort();

        var ctx = new SpringApplicationBuilder(LoadBalancerApp.class)
                .properties("server.port=" + port)
                .properties("http-exchange.client-type=" + clientType)
                .properties("spring.cloud.loadbalancer.retry.enabled=false")
                .run();

        UserApi userApi = ctx.getBean(UserApi.class);

        int success = 0;
        int failure = 0;
        for (int i = 0; i < 4; i++) {
            try {
                userApi.getById("1");
                success++;
            } catch (Exception e) {
                failure++;
            }
        }

        assertThat(success).isEqualTo(2);
        assertThat(failure).isEqualTo(2);

        ctx.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest_client", "rest_template", "web_client"})
    void testLoadBalancer_whenDisabled(String clientType) {
        int port = getRandomPort();

        var ctx = new SpringApplicationBuilder(LoadBalancerApp.class)
                .properties("server.port=" + port)
                .properties("http-exchange.client-type=" + clientType)
                .properties("spring.cloud.loadbalancer.enabled=false")
                .run();

        UserApi userApi = ctx.getBean(UserApi.class);

        int success = 0;
        int failure = 0;
        for (int i = 0; i < 4; i++) {
            try {
                userApi.getById("1");
                success++;
            } catch (Exception e) {
                failure++;
            }
        }

        assertThat(success).isZero();
        assertThat(failure).isEqualTo(4);

        ctx.close();
    }

    @SneakyThrows
    private static int getRandomPort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }
}
