package io.github.danielliu1123.order.api;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("http://localhost:8080")
public interface OrderApi {

    @GetExchange("/order")
    String getOrder();
}
