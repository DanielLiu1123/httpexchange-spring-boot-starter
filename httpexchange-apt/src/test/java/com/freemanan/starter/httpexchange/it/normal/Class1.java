package com.freemanan.starter.httpexchange.it.normal;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
public class Class1 {

    public interface Api6 {
        @GetExchange
        String get();
    }

    @HttpExchange
    public interface Api7 {
        String get();
    }

    public interface Api8 {
        String get();
    }
}
