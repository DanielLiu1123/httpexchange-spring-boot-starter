package io.github.danielliu1123.httpexchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Tests for {@link SpringBootVersionIncompatibleFailureAnalyzer}.
 */
class SpringBootVersionIncompatibleFailureAnalyzerTest {

    @Test
    void testAnalyze() {
        // Given
        SpringBootVersionIncompatibleFailureAnalyzer analyzer = new SpringBootVersionIncompatibleFailureAnalyzer();
        SpringBootVersionIncompatibleException exception = new SpringBootVersionIncompatibleException("3.4.9", "3.5.0");

        // When
        FailureAnalysis analysis = analyzer.analyze(exception);

        // Then
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription())
                .contains("requires Spring Boot 3.5.0 or higher")
                .contains("but found 3.4.9");
        assertThat(analysis.getAction())
                .contains(
                        "If you're using a Spring Boot version < 3.5.0, please stick with httpexchange-spring-boot-starter version 3.4.x")
                .contains("Spring Boot 3.5.0 introduced extensive internal refactoring")
                .contains("https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/releases/tag/v3.5.0");
        assertThat(analysis.getCause()).isSameAs(exception);
    }
}
