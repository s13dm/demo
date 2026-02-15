package com.demo.order.dto;

import java.time.LocalDateTime;

import com.demo.order.entity.DeliveryStatus;

public record CreateOrderResponse(
        Long orderId,
        Long userId,
        String productName,
        int quantity,
        String shippingAddress,
        DeliveryStatus deliveryStatus,
        LocalDateTime orderedAt
) {
}
