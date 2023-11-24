package io.github.danielliu1123.user.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

/**
 * @author Freeman
 */
public interface UserHobbyApi {

    @GetExchange("https://my-json-server.typicode.com/typicode/demo/posts/{id}")
    ResponseEntity<Map<String, String>> getUserHobby(@PathVariable("id") String id);
}
