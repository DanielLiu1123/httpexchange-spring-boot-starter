package com.example.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import com.example.api.UserApi;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest(webEnvironment = DEFINED_PORT)
class QuickStartAppTest {

    @Autowired
    UserApi userApi;

    @Test
    void testGetUser_whenArgIsValid() {
        UserApi.UserDTO user = userApi.getById("1");
        assertThat(user.id()).isEqualTo("1");
        assertThat(user.name()).isEqualTo("Freeman");
        assertThat(user.hobbies()).containsExactly("Coding", "Reading");
    }

    @Test
    void testGetUser_whenArgIsInvalid() {
        assertThatCode(() -> userApi.getById("111111"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("getById.id: length must be between 0 and 5");
    }

    @Test
    void testGetUserByName_whenControllerNotImplement() {
        assertThatCode(() -> userApi.getByName("Freeman"))
                .isInstanceOf(HttpServerErrorException.NotImplemented.class)
                .hasMessageContaining("501"); // Not implemented
    }
}
