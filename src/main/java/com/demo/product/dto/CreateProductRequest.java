package com.demo.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(
        @NotBlank String name,
        @Min(0) int price,
        @Min(0) int stock
) {
}
