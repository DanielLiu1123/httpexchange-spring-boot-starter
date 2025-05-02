package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Integration test for {@link SpringBootVersionIncompatibleFailureAnalyzer}.
 * This test verifies that the failure analyzer properly formats and displays
 * error messages when the application is started with an incompatible Spring Boot version.
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBootVersionIncompatibleFailureAnalyzerIT {

    @Test
    void shouldDisplayFormattedErrorMessage_whenSpringBootVersionIsIncompatible(CapturedOutput output) {
        try (MockedStatic<SpringBootVersion> mockedStatic = mockStatic(SpringBootVersion.class)) {
            // Mock Spring Boot version to be 3.4.5
            mockedStatic.when(SpringBootVersion::getVersion).thenReturn("3.4.5");

            try (var ignored = SpringApplication.run(TestApplication.class)) {
            } catch (Exception e) {
                // Expected exception, we're testing the error message format
            }
        }

        // Verify the error message contains the expected information
        assertThat(output.getOut())
                .contains("Description:")
                .contains(
                        "The current version of httpexchange-spring-boot-starter requires Spring Boot 3.5.0 or higher, but found 3.4.5")
                .contains("Action:")
                .contains(
                        "If you're using a Spring Boot version < 3.5.0, please stick with httpexchange-spring-boot-starter version 3.4.x")
                .contains("Spring Boot 3.5.0 introduced extensive internal refactoring")
                .contains("https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/releases/tag/v3.5.0");
    }

    @EnableAutoConfiguration
    @EnableExchangeClients
    static class TestApplication {}
}
