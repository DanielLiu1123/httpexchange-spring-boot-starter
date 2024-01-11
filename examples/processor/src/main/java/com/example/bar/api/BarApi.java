package com.example.bar.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface BarApi {

    @GetExchange("/bars/{id}")
    String get(@PathVariable("id") String id);
}
