package com.example.bar.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface RequestMappingBarApi {

    @GetMapping("/bars/{id}")
    String get(@PathVariable("id") String id);
}
