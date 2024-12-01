package issues.issue73;

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
    public UserApi userApi(RestClient.Builder builder) {
        builder.baseUrl("http://localhost:" + Issue73Test.port);
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(UserApi.class);
    }
}
