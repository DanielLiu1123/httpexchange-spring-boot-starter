package com.example.order.api;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface OrderApi {

    @GetExchange("/users/{userId}/orders")
    List<Order> listOrderByUserId(@PathVariable("userId") String userId);
}
