package com.example.api;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@Validated
@HttpExchange("/user")
public interface UserApi {

    @GetExchange("/{id}")
    UserDTO getUser(@PathVariable("id") @NotBlank @Length(max = 5) String id);

    @GetExchange("/byName/{name}")
    UserDTO getUserByName(@PathVariable("name") @NotBlank String name);
}
