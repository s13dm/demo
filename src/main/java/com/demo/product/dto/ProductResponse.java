package com.demo.product.dto;

public record ProductResponse(
        Long productId,
        String name,
        int price,
        int stock
) {
}
