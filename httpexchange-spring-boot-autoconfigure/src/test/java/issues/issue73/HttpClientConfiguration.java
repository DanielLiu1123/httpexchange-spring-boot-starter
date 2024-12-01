package issues.issue73;

import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 * @since 2024/12/1
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfiguration {

    @Bean
    public UserApi userApi(RestClient.Builder builder, WebServerApplicationContext ctx) {
        builder.baseUrl("http://localhost:" + ctx.getWebServer().getPort());
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(UserApi.class);
    }
}
