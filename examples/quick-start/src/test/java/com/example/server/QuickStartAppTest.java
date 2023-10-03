package com.example.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.api.UserApi;
import com.example.api.UserDTO;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class QuickStartAppTest {

    @Autowired
    UserApi userApi;

    @Test
    void testGetUser_whenArgIsValid() {
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
}
