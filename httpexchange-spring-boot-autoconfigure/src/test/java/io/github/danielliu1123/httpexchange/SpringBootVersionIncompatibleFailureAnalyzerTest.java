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
        SpringBootVersionIncompatibleException exception = new SpringBootVersionIncompatibleException("3.5.4", "4.0.0");

        // When
        FailureAnalysis analysis = analyzer.analyze(exception);

        // Then
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription())
                .contains("requires Spring Boot 4.0.0 or higher")
                .contains("but found 3.5.4");
        assertThat(analysis.getAction()).contains("Please upgrade your Spring Boot version to at least 4.0.0");
        assertThat(analysis.getCause()).isSameAs(exception);
    }
}
