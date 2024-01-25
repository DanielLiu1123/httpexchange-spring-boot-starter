package com.example.baz.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface BazApi {

    @GetExchange("/bars/{id}")
    String get(@PathVariable("id") String id);
}
