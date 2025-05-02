package io.github.danielliu1123.httpexchange;

/**
 * Exception thrown when the Spring Boot version is incompatible with the httpexchange-spring-boot-starter.
 *
 * @author Freeman
 */
class SpringBootVersionIncompatibleException extends RuntimeException {

    private final String currentVersion;
    private final String requiredVersion;

    /**
     * Constructs a new exception with the current and required Spring Boot versions.
     *
     * @param currentVersion the current Spring Boot version
     * @param requiredVersion the minimum required Spring Boot version
     */
    public SpringBootVersionIncompatibleException(String currentVersion, String requiredVersion) {
        super("Spring Boot version " + currentVersion + " is incompatible with httpexchange-spring-boot-starter. "
                + "Minimum required version is " + requiredVersion);
        this.currentVersion = currentVersion;
        this.requiredVersion = requiredVersion;
    }

    /**
     * Gets the current Spring Boot version.
     *
     * @return the current Spring Boot version
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Gets the required Spring Boot version.
     *
     * @return the required Spring Boot version
     */
    public String getRequiredVersion() {
        return requiredVersion;
    }
}
