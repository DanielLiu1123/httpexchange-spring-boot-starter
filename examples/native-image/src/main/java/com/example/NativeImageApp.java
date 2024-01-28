package com.example;

import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import io.github.danielliu1123.httpexchange.RequestConfigurator;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.service.annotation.GetExchange;

@SpringBootApplication
@EnableExchangeClients
public class NativeImageApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(NativeImageApp.class, args);
        PostApi postApi = ctx.getBean(PostApi.class);

        postApi.list().forEach(System.out::println);
        postApi.withTimeout(2000).list().forEach(System.out::println);

        System.setProperty("http-exchange.base-url", "http://jsonplaceholder.typicode.com");

        ctx.publishEvent(new RefreshEvent(ctx, null, null));

        postApi.list().forEach(System.out::println);
        postApi.withTimeout(2000).list().forEach(System.out::println);

        System.clearProperty("http-exchange.base-url");
    }

    public record Post(Integer id, String title) {}

    public interface PostApi extends RequestConfigurator<PostApi> {
        @GetExchange("/typicode/demo/posts")
        List<Post> list();
    }
}
