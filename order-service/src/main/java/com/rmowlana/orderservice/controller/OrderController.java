package com.rmowlana.orderservice.controller;

import com.rmowlana.avroschemaandregistry.dto.Order;
import com.rmowlana.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/order")
    public String placeOrder (@RequestBody Order order) {
        orderService.placeOrder(order);
        return "Order Received!";
    }
}
