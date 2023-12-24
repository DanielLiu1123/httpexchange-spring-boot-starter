package com.example.api;

import com.example.api.dto.UserDTO;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/user/blocking")
public interface UserApi {

    @GetExchange("/{id}")
    UserDTO get(@PathVariable("id") String id);

    @GetExchange
    List<UserDTO> list();
}
