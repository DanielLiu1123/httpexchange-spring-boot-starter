package com.example.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/users")
public interface UserApi {

    record UserDTO(String id, String name) {}

    @GetExchange("/{id}")
    UserDTO get(@PathVariable("id") String id);
}
