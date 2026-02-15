package com.demo.user.dto;

import java.util.List;

import com.demo.order.dto.CreateOrderResponse;

public record UserOrdersResponse(
        Long userId,
        String userName,
        int totalOrders,
        List<CreateOrderResponse> orders
) {
}
