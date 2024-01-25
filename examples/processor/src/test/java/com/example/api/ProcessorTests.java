package com.example.api;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * @author Freeman
 */
class ProcessorTests {

    @Test
    void testGenerateCode() {
        assertThatCode(() -> Class.forName("com.example.foo.api.generated.AbstractFooApiImpl"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.example.bar.api.generated.AbstractBarApiImpl"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.example.bar.api.generated.AbstractRequestMappingBarApiImpl"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.example.baz.api.generated.AbstractBazApiImpl"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
