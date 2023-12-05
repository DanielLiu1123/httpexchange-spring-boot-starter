package com.example;

import io.github.danielliu1123.httpexchange.EnableExchangeClients;
import java.util.List;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableExchangeClients
@RestController
public class LoadBalancerApp implements UserApi {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LoadBalancerApp.class)
                .properties("server.port=0")
                .run(args);
    }

    @Override
    public UserApi.UserDTO getById(String id) {
        return new UserApi.UserDTO(id, "Freeman", List.of("Coding", "Reading"));
    }
}
