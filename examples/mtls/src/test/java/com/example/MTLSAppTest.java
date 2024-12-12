package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.order.api.OrderApi;
import com.example.user.api.UserApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("tbd")
class MTLSAppTest {

    @Autowired
    UserApi userApi;

    @Autowired
    OrderApi orderApi;

    @Test
    void testPostApi() {
        assertThat(userApi.getUser("1")).isNotNull();
        assertThat(orderApi.listOrderByUserId("1")).isNotNull();
    }
}
