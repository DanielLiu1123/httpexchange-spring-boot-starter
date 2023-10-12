package com.example.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Only used for server side implementation.
 */
public abstract class UserApiBase implements UserApi {

    @Override
    public UserDTO getUser(String id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public UserDTO getUserByName(String name) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
