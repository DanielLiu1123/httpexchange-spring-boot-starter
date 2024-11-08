package com.example;

import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

@SpringBootApplication
@EnableExchangeClients
public class NativeImageApp {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(NativeImageApp.class, args);

        var postApi = ctx.getBean(PostApi.class);

        postApi.list().forEach(System.out::println);

        System.out.println(postApi.get(1));

        if (System.getenv("CI") != null) {
            ctx.close();
        }
    }

    public record Post(Integer id, String title) {}

    public interface PostApi {
        @GetExchange("/typicode/demo/posts")
        List<Post> list();

        @GetExchange("/typicode/demo/posts/{id}")
        Post get(@PathVariable("id") int id);
    }
}
