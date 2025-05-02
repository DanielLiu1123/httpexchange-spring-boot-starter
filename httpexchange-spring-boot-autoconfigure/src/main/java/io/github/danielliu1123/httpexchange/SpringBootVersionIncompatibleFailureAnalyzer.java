package io.github.danielliu1123.httpexchange;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@link org.springframework.boot.diagnostics.FailureAnalyzer} that analyzes
 * {@link SpringBootVersionIncompatibleException}.
 *
 * @author Freeman
 */
class SpringBootVersionIncompatibleFailureAnalyzer
        extends AbstractFailureAnalyzer<SpringBootVersionIncompatibleException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SpringBootVersionIncompatibleException cause) {
        return new FailureAnalysis(
                String.format(
                        "The current version of httpexchange-spring-boot-starter requires Spring Boot %s or higher, but found %s.",
                        cause.getRequiredVersion(), cause.getCurrentVersion()),
                String.format(
                        """
                        If you're using a Spring Boot version < %s, please stick with httpexchange-spring-boot-starter version 3.4.x.

                        Spring Boot 3.5.0 introduced extensive internal refactoring. To reduce maintenance costs, backward compatibility has been dropped.

                        For more information, see: https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/releases/tag/v3.5.0
                        """,
                        cause.getRequiredVersion()),
                cause);
    }
}
