package com.example.user.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface UserApi {

    @GetExchange("/users/{id}")
    User getUser(@PathVariable("id") String id);
}
