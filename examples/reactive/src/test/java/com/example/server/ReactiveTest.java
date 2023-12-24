package com.example.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import com.example.api.UserApi;
import com.example.api.UserReactiveApi;
import com.example.api.dto.UserDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = DEFINED_PORT)
class ReactiveTest {

    @Autowired
    UserApi userApi;

    @Autowired
    UserReactiveApi userReactiveApi;

    @Test
    void testBlockingApi() {
        assertThat(userApi).isNotInstanceOf(ReactiveApp.UserApiImpl.class);

        UserDTO user = userApi.get("1");

        assertThat(user.id()).isEqualTo("1");
        assertThat(user.name()).isEqualTo("Freeman");
        assertThat(user.hobbies()).containsExactly("Coding", "Reading");
    }

    @Test
    void testReactiveApi() {
        assertThat(userReactiveApi).isNotInstanceOf(ReactiveApp.UserReactiveApiImpl.class);

        UserDTO user = userReactiveApi.get("1").block();

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo("1");
        assertThat(user.name()).isEqualTo("Freeman");
        assertThat(user.hobbies()).containsExactly("Coding", "Reading");

        List<UserDTO> users = userReactiveApi.list().collectList().block();

        assertThat(users).isNotNull().hasSize(2);
        assertThat(users.get(0).id()).isEqualTo("1");
        assertThat(users.get(0).name()).isEqualTo("Freeman");
        assertThat(users.get(0).hobbies()).containsExactly("Coding", "Reading");
        assertThat(users.get(1).id()).isEqualTo("2");
        assertThat(users.get(1).name()).isEqualTo("Jack");
        assertThat(users.get(1).hobbies()).containsExactly("Coding", "Gaming");
    }
}
