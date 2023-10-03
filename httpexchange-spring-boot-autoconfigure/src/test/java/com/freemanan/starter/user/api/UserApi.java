package com.freemanan.starter.user.api;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("http://localhost:8080")
public interface UserApi {

    @GetExchange("/user")
    String getUser();
}
