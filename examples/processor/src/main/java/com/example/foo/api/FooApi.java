package com.example.foo.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface FooApi {

    @GetExchange("/foos/{id}")
    String get(@PathVariable("id") String id);
}
