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
 * <p> Examples:
 *
 * <p> Scan the package of the annotated class:
 * <pre>{@code
 * @EnableExchangeClients
 * }</pre>
 *
 * <p> Scan the package of the specified {@link #basePackages} (not include the package of annotated class):
 * <pre>{@code
 * @EnableExchangeClients("org.my.pkg")
 * }</pre>
 *
 * <p> Register specified clients (don't scan any packages):
 * <pre>{@code
 * @EnableExchangeClients(clients = {FooApi.class})
 * }</pre>
 *
 * <p> Scan specified {@link #basePackages} and register specified clients:
 * <pre>{@code
 * @EnableExchangeClients(basePackages = "org.my.pkg", clients = {FooApi.class})
 * }</pre>
 *
 * <p color="orange"> NOTE: scanning packages will increase the program startup time, you can sacrifice some flexibility and use the {@link #clients} attribute to specify the interfaces that need to be registered as beans.
 *
 * @author Freeman
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({ExchangeClientsRegistrar.class})
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
     * <p> clients and {@link #basePackages} <strong>can</strong> be used together.
     *
     * @return the interfaces to register as HttpExchange client beans.
     */
    Class<?>[] clients() default {};
}
