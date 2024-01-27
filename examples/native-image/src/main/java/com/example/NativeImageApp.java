package com.example;

import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import io.github.danielliu1123.httpexchange.RequestConfigurator;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@SpringBootApplication
@EnableExchangeClients /*(clients = NativeImageApp.PostApi.class)*/
// @EnableFeignClients
public class NativeImageApp {

    public static void main(String[] args) {
        SpringApplication.run(NativeImageApp.class, args);
    }

    public record Post(Integer id, String title) {}

    @HttpExchange("https://my-json-server.typicode.com")
    public interface PostApi {
        @GetExchange("/typicode/demo/posts")
        List<Post> list();
    }

    @RequestMapping("https://my-json-server.typicode.com")
    //    @FeignClient(name = "postApi2", url = "https://my-json-server.typicode.com", contextId = "PostApi2")
    public interface PostApi2 extends RequestConfigurator<PostApi2> {
        @GetMapping("/typicode/demo/posts")
        List<Post> list();
    }

    @Bean
    ApplicationRunner runner(PostApi postApi, PostApi2 postApi2) {
        return args -> {
            postApi.list().forEach(System.out::println);
            postApi2.list().forEach(System.out::println);
        };
    }
}
