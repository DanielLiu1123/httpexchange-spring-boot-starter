package com.example.server.controller;

import com.example.api.UserApiBase;
import com.example.api.UserDTO;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController extends UserApiBase {
    @Override
    public UserDTO getUser(String id) {
        return new UserDTO().setId(id).setName("Freeman").setHobbies(List.of("Coding", "Reading"));
    }
}
