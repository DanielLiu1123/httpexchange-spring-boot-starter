package com.example;

import static com.example.NativeImageApp.PostApi;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NativeImageAppTest {

    @Autowired
    PostApi postApi;

    @Test
    void testPostApi() {
        assertThat(postApi.list()).isNotEmpty();
    }
}
