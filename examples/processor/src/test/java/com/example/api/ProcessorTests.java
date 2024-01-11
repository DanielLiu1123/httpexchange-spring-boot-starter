package com.example.api;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * @author Freeman
 */
class ProcessorTests {

    @Test
    void testGenerateCode() {
        assertThatCode(() -> Class.forName("com.example.foo.api.AbstractFooApiImpl"))
                .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("com.example.bar.api.AbstractBarApiImpl"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
