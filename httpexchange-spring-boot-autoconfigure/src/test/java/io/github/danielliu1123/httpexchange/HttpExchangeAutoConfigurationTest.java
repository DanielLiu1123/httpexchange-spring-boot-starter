package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
            assertThat(ctx).hasSingleBean(CommandLineRunner.class);
        });
    }

    @Test
    void testEnableIsFalse() {
        runner.withPropertyValues("http-exchange.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(HttpClientBeanDefinitionRegistry.class);
            assertThat(ctx).doesNotHaveBean(BeanParamArgumentResolver.class);
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

    @Test
    void shouldThrowException_whenSpringBootVersionIsLessThan400() {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("3.5.4");

            // Create an instance of HttpExchangeAutoConfiguration
            HttpExchangeAutoConfiguration config = new HttpExchangeAutoConfiguration();

            // Should throw exception when afterPropertiesSet is called
            assertThatExceptionOfType(SpringBootVersionIncompatibleException.class)
                    .isThrownBy(config::afterPropertiesSet)
                    .withMessage(
                            "Spring Boot version 3.5.4 is incompatible with httpexchange-spring-boot-starter. Minimum required version is 4.0.0");
        }
    }

    @Test
    void shouldNotThrowException_whenSpringBootVersionIsEqualTo400() {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("4.0.0");

            // Create an instance and call afterPropertiesSet
            HttpExchangeAutoConfiguration config = new HttpExchangeAutoConfiguration();
            assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldNotThrowException_whenSpringBootVersionIsGreaterThan400() {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("4.1.2");

            // Create an instance and call afterPropertiesSet
            HttpExchangeAutoConfiguration config = new HttpExchangeAutoConfiguration();
            assertThatCode(config::afterPropertiesSet).doesNotThrowAnyException();
        }
    }
}
