package com.example.order.server;

import com.example.order.api.Order;
import com.example.order.api.OrderApi;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderServer implements OrderApi {
    @Override
    public List<Order> listOrderByUserId(String userId) {
        return List.of(new Order("1", userId, 100.00), new Order("2", userId, 200.00));
    }
}
