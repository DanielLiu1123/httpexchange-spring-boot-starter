package com.example.api;

import com.example.api.dto.UserDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@HttpExchange("/user/reactive")
public interface UserReactiveApi {

    @GetExchange("/{id}")
    Mono<UserDTO> get(@PathVariable("id") String id);

    @GetExchange
    Flux<UserDTO> list();
}
