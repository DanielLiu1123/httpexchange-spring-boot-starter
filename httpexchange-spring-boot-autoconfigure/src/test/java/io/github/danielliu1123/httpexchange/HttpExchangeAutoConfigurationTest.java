package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * {@link HttpExchangeAutoConfiguration} tester.
 */
class HttpExchangeAutoConfigurationTest {

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpExchangeAutoConfiguration.class));

    @Test
    void testDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(HttpClientBeanDefinitionRegistry.class);
            assertThat(ctx).hasSingleBean(BeanParamArgumentResolver.class);
            assertThat(ctx).hasSingleBean(ClientHttpRequestFactory.class);
            assertThat(ctx).hasSingleBean(CommandLineRunner.class);
        });
    }

    @Test
    void testEnableIsFalse() {
        runner.withPropertyValues("http-exchange.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(HttpClientBeanDefinitionRegistry.class);
            assertThat(ctx).doesNotHaveBean(BeanParamArgumentResolver.class);
            assertThat(ctx).doesNotHaveBean(ClientHttpRequestFactory.class);
            assertThat(ctx).doesNotHaveBean(CommandLineRunner.class);
        });
    }

    @Test
    void testWarnUnusedConfig() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(CommandLineRunner.class);
        });

        runner.withPropertyValues("http-exchange.warn-unused-config-enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CommandLineRunner.class);
                });

        runner.withPropertyValues("http-exchange.warn-unused-config-enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(CommandLineRunner.class);
                });
    }
}
