package com.example.api;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@Validated
@HttpExchange("/user")
public interface UserApi {

    @GetExchange("/{id}")
    default UserDTO getUser(@PathVariable("id") @NotBlank @Length(max = 5) String id) {
        // Giving default implementation for avoiding compilation errors
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
