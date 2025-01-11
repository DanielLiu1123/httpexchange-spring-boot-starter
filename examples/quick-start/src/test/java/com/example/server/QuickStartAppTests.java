package com.example.server;

import com.example.api.UserApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

class QuickStartAppTests {

    @Test
    void test1() {
        var ctx = new SpringApplicationBuilder(QuickStartApp.class).run();
        var userApi = ctx.getBean(UserApi.class);
        UserApi.UserDTO user = userApi.getById("1");
        System.out.println(user);
    }
}
