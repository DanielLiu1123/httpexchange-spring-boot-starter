package com.example.server;

import com.example.api.UserApi;
import com.example.api.serverbase.UserApiBase;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class QuickStartApp extends UserApiBase {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }

    @Override
    public UserApi.UserDTO getById(String id) {
        return new UserApi.UserDTO(id, "Freeman", List.of("Coding", "Reading"));
    }
}
