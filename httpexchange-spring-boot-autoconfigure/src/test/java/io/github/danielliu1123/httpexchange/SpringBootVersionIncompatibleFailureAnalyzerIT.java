package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        try (var mocker = mockStatic(SpringBootVersion.class)) {
            // Mock Spring Boot version to be 3.5.4
            mocker.when(SpringBootVersion::getVersion).thenReturn("3.5.4");

            try (var ignored = SpringApplication.run(TestApplication.class)) {
            } catch (Exception e) {
                // Expected exception, we're testing the error message format
            }
        }

        // Verify the error message contains the expected information
        assertThat(output.getOut())
                .contains("Description:")
                .contains(
                        "The current version of httpexchange-spring-boot-starter requires Spring Boot 4.0.0 or higher, but found 3.5.4.")
                .contains("Action:")
                .contains("Please upgrade your Spring Boot version to at least 4.0.0.");
    }

    @EnableAutoConfiguration
    @EnableExchangeClients
    static class TestApplication {}
}
