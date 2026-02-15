package com.demo.order.dto;

public record CreateUserResponse(
        Long userId,
        String name,
        String email
) {
}
