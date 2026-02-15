package com.demo.order.dto;

import java.time.LocalDateTime;

import com.demo.order.entity.DeliveryStatus;

public record DeliveryStatusResponse(
        Long orderId,
        DeliveryStatus deliveryStatus,
        LocalDateTime orderedAt
) {
}
