package io.github.danielliu1123.httpexchange.it.normal;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("/api")
interface Api5 {

    @GetExchange
    String get();
}
