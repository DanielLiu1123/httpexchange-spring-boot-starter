package com.freemanan.starter.httpexchange.it.normal;

import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
public interface Api4 {

    @HttpExchange("/api")
    String get();

    interface InnerApi {

        @HttpExchange("/innerApi")
        String get();
    }

    interface InnerApi2 {

        String get();
    }
}
