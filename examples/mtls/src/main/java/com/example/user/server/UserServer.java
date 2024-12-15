package com.example.user.server;

import com.example.user.api.User;
import com.example.user.api.UserApi;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserServer implements UserApi {
    @Override
    public User getUser(String id) {
        return new User(id, "John");
    }
}
