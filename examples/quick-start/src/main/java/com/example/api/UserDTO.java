package com.example.api;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserDTO {
    private String id;
    private String name;
    private List<String> hobbies;
}
