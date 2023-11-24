package io.github.danielliu1123.httpexchange.it.normal;

import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("/api")
public interface Api3 {

    String get();
}
