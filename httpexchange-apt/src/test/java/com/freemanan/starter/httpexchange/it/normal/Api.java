package com.freemanan.starter.httpexchange.it.normal;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("/api")
public interface Api {

    @GetExchange
    String get();
}
