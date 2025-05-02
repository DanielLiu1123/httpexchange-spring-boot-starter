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
    void shouldThrowException_whenSpringBootVersionIsLessThan350() {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            // Mock Spring Boot version to be 3.4.9
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("3.4.9");

            // Should throw exception when version is less than 3.5.0
            assertThatExceptionOfType(SpringBootVersionIncompatibleException.class)
                    .isThrownBy(HttpExchangeAutoConfiguration::new)
                    .withMessage(
                            "Spring Boot version 3.4.9 is incompatible with httpexchange-spring-boot-starter. Minimum required version is 3.5.0")
                    .satisfies(ex -> {
                        assertThat(ex.getCurrentVersion()).isEqualTo("3.4.9");
                        assertThat(ex.getRequiredVersion()).isEqualTo("3.5.0");
                    });
        }
    }

    @Test
    void shouldNotThrowException_whenSpringBootVersionIsEqualTo350() {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            // Mock Spring Boot version to be 3.5.0
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("3.5.0");

            // Should not throw exception when version is 3.5.0
            assertThatCode(HttpExchangeAutoConfiguration::new).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldNotThrowException_whenSpringBootVersionIsGreaterThan350() {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            // Mock Spring Boot version to be 3.6.0
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("3.6.0");

            // Should not throw exception when version is greater than 3.5.0
            assertThatCode(HttpExchangeAutoConfiguration::new).doesNotThrowAnyException();
        }
    }
}
