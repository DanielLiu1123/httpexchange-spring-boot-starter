package issues.issue73;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Freeman
 * @see <a href="https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/pull/73">Fixes use manually registered bean not works</a>
 */
class Issue73Test {

    static int port = findAvailableTcpPort();

    @Test
    void useManualRegisteredBean_whenManualRegisteredBeanExists() {
        try (var ctx = new SpringApplicationBuilder(CfgWithHttpClientConfiguration.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=" + port)
                .properties("http-exchange.base-packages=" + UserApi.class.getPackageName())
                .properties("http-exchange.base-url=localhost:" + (port - 1)) // wrong base-url
                .run()) {

            var api = ctx.getBean(UserApi.class);

            var username = api.getUsername("1");

            // Got the correct result, means the manual registered bean works
            assertThat(username).isEqualTo("Hello 1");
        }
    }

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBeanAndUsingWrongBaseUrl_thenThrowException() {
        try (var ctx = new SpringApplicationBuilder(CfgWithoutHttpClientConfiguration.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=" + port)
                .properties("http-exchange.base-packages=" + UserApi.class.getPackageName())
                .properties("http-exchange.base-url=localhost:" + (port - 1)) // wrong base-url
                .run()) {

            var api = ctx.getBean(UserApi.class);

            assertThatCode(() -> api.getUsername("1"))
                    .isInstanceOf(ResourceAccessException.class)
                    .hasMessageContaining("I/O error");
        }
    }

    @Test
    void useAutoRegisteredBean_whenNoManualRegisteredBeanAndUsingCorrectBaseUrl_thenGotCorrectResult() {
        var port = findAvailableTcpPort();
        try (var ctx = new SpringApplicationBuilder(CfgWithoutHttpClientConfiguration.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=" + port)
                .properties("http-exchange.base-packages=" + UserApi.class.getPackageName())
                .properties("http-exchange.base-url=localhost:" + port) // correct base-url
                .run()) {

            var api = ctx.getBean(UserApi.class);

            var username = api.getUsername("1");

            assertThat(username).isEqualTo("Hello 1");
        }
    }
}
