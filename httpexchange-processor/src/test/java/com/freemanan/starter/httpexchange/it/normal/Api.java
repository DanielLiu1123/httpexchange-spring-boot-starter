package com.freemanan.starter.httpexchange.it.normal;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * @author Freeman
 */
@HttpExchange("/api")
public interface Api {

    @GetExchange
    String get();

    @PostExchange
    default String post() {
        throw new UnsupportedOperationException();
    }
}
