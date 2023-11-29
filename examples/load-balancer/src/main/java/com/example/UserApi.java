package com.example;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/user")
public interface UserApi {
    record UserDTO(String id, String name, List<String> hobbies) {}

    @GetExchange("/getById/{id}")
    UserDTO getById(@PathVariable("id") String id);
}
