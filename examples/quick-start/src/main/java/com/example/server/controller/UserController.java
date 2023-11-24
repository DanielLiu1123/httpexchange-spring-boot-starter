package com.example.server.controller;

import com.example.api.UserApiBase;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController extends UserApiBase {
    @Override
    public UserDTO getById(String id) {
        return new UserDTO(id, "Freeman", List.of("Coding", "Reading"));
    }
}
