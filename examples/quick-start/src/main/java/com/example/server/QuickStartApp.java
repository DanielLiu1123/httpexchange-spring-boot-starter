package com.example.server;

import com.example.api.UserApi;
import com.freemanan.starter.httpexchange.EnableExchangeClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
@EnableExchangeClients("com.example.api")
public class QuickStartApp {
    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }

    @Autowired
    UserApi userApi;

    @Bean
    ApplicationRunner runner() {
        return args -> log.info("{}", userApi.getUser("1"));
    }
}
