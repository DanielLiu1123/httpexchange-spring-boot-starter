package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @EnableExchangeClients
// @ImportRuntimeHints(HttpExchangeHints.class)
public class NativeImageApp {

    public static void main(String[] args) {
        SpringApplication.run(NativeImageApp.class, args);
    }

    //    @HttpExchange("https://my-json-server.typicode.com")
    //    interface PostApi {
    //        record Post(Integer id, String title) {
    //        }
    //
    //        @GetExchange("/typicode/demo/posts")
    //        List<Post> list();
    //    }
}
