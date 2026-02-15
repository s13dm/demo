package com.demo.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.CreateUserRequest;
import com.demo.order.dto.CreateUserResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.service.OrderDeliveryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    public OrderDeliveryController(OrderDeliveryService orderDeliveryService) {
        this.orderDeliveryService = orderDeliveryService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse registerUser(@Valid @RequestBody CreateUserRequest request) {
        return orderDeliveryService.registerUser(request);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderDeliveryService.placeOrder(request);
    }

    @GetMapping("/orders/{orderId}/delivery")
    public DeliveryStatusResponse checkDeliveryStatus(@PathVariable Long orderId) {
        return orderDeliveryService.checkDeliveryStatus(orderId);
    }

    @PatchMapping("/orders/{orderId}/delivery")
    public DeliveryStatusResponse updateDeliveryStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request
    ) {
        return orderDeliveryService.updateDeliveryStatus(orderId, request);
    }
}
