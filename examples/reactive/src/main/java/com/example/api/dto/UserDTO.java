package com.example.api.dto;

import java.util.List;

public record UserDTO(String id, String name, List<String> hobbies) {}
