package com.example.server;

import com.example.api.UserApi;
import com.example.api.UserReactiveApi;
import com.example.api.dto.UserDTO;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ReactiveApp {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveApp.class, args);
    }

    @RestController
    static class UserApiImpl implements UserApi {

        @Override
        public UserDTO get(String id) {
            return new UserDTO(id, "Freeman", List.of("Coding", "Reading"));
        }

        @Override
        public List<UserDTO> list() {
            return List.of(
                    new UserDTO("1", "Freeman", List.of("Coding", "Reading")),
                    new UserDTO("2", "Jack", List.of("Coding", "Gaming")));
        }
    }

    @RestController
    static class UserReactiveApiImpl implements UserReactiveApi {

        @Override
        public Mono<UserDTO> get(String id) {
            return Mono.just(new UserDTO(id, "Freeman", List.of("Coding", "Reading")));
        }

        @Override
        public Flux<UserDTO> list() {
            return Flux.just(
                    new UserDTO("1", "Freeman", List.of("Coding", "Reading")),
                    new UserDTO("2", "Jack", List.of("Coding", "Gaming")));
        }
    }
}
