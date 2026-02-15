package com.demo.order.dto;

import java.util.List;

public record UserDeliveryStatusResponse(
        Long userId,
        String userName,
        String email,
        int totalOrders,
        List<DeliveryStatusResponse> deliveries
) {
}
