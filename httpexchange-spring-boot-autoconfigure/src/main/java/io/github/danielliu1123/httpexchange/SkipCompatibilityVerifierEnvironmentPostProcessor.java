package io.github.danielliu1123.httpexchange;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ObjectUtils;

/**
 * TODO(Freeman): Remove this class after the Spring Cloud compatibility version is released.
 *
 * @author Freeman
 */
public class SkipCompatibilityVerifierEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String pkg = application.getMainApplicationClass().getPackage().getName();
        if (ObjectUtils.isEmpty(pkg)
                || pkg.startsWith(EnableExchangeClients.class.getPackage().getName())
                || pkg.startsWith("com.example")) {
            return;
        }

        environment
                .getPropertySources()
                .addLast(new MapPropertySource(
                        "httpExchangeStarterSkipCompatibilityVerifier",
                        Map.of("spring.cloud.compatibility-verifier.enabled", "false")));
    }
}
