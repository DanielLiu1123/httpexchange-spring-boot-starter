package com.example.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.api.UserApi;
import com.example.api.UserDTO;
import com.example.server.controller.UserController;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class QuickStartAppTest {

    @Autowired
    UserApi userApi;

    @Test
    void testGetUser_whenArgIsValid() {
        assertThat(userApi).isNotInstanceOf(UserController.class);

        UserDTO user = userApi.getUser("1");
        assertThat(user.getId()).isEqualTo("1");
        assertThat(user.getName()).isEqualTo("Freeman");
        assertThat(user.getHobbies()).containsExactly("Coding", "Reading");
    }

    @Test
    void testGetUser_whenArgIsInvalid() {
        assertThatCode(() -> userApi.getUser("111111"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("getUser.id: length must be between 0 and 5");
    }

    @Test
    void testGetUserByName_whenControllerNotImplement() {
        assertThatCode(() -> userApi.getUserByName("Freeman"))
                .isInstanceOf(HttpServerErrorException.NotImplemented.class)
                .hasMessageContaining("501"); // Not implemented
    }
}
