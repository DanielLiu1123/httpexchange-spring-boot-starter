package issues.issue73;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Freeman
 * @since 2024/12/1
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@RestController
@RequestMapping("/users")
class CfgWithoutHttpClientConfiguration {
    @GetMapping("/{id}")
    public String getUsername(@PathVariable("id") String id) {
        return "Hello " + id;
    }
}
