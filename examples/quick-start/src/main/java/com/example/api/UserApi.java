package com.example.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@Validated
@HttpExchange("/user")
public interface UserApi {
    record UserDTO(String id, String name, List<String> hobbies) {}

    @GetExchange("/getById/{id}")
    UserDTO getById(@PathVariable("id") @NotBlank @Length(max = 5) String id);

    @GetExchange("/getByName/{name}")
    UserDTO getByName(@PathVariable("name") @NotBlank String name);
}
