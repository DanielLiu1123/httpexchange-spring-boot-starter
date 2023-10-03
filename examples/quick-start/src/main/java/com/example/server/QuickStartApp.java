package com.example.server;

import com.freemanan.starter.httpexchange.EnableExchangeClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableExchangeClients("com.example.api")
public class QuickStartApp {
    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }
}
