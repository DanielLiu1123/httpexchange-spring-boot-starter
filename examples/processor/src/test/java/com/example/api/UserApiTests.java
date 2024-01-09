package com.example.api;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * @author Freeman
 */
class UserApiTests {

    @Test
    void testGenerateCode() {
        assertThatCode(() -> Class.forName("com.example.api.AbstractUserApiImpl"))
                .doesNotThrowAnyException();
    }
}
