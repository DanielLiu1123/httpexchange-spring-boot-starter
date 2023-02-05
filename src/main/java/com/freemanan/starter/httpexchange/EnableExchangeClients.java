package com.freemanan.starter.httpexchange;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Enable auto scan {@link HttpExchange} interfaces, and register them as {@link HttpExchange} client beans.
 *
 * <p> Basic usage:
 * <pre>{@code
 * @SpringBootApplication
 * @EnableExchangeClients("org.my.pkg")
 * public class App {}
 * }</pre>
 *
 * @author Freeman
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import(ExchangeClientsRegistrar.class)
public @interface EnableExchangeClients {
    /**
     * Scan base packages.
     *
     * <p> Scan the package of the annotated class by default.
     * <p> Alias for the {@link #basePackages()} attribute.
     *
     * @return the base packages to scan
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Alias for the {@link #value()} attribute.
     *
     * @return the base packages to scan
     * @see #value()
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * The classes to register as HttpExchange client beans.
     *
     * <p> NOTE: when the clients attribute is present, the {@link #basePackages} attribute will be ignored.
     *
     * @return the classes to register as HttpExchange client beans.
     */
    Class<?>[] clients() default {};
}
