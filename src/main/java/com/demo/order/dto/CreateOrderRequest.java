package com.demo.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotBlank String productName,
        @Min(1) int quantity,
        @NotBlank String shippingAddress
) {
}
