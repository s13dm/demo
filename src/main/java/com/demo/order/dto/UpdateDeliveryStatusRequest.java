package com.demo.order.dto;

import com.demo.order.entity.DeliveryStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryStatusRequest(
        @NotNull DeliveryStatus deliveryStatus
) {
}
