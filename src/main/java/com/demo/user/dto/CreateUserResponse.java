package com.demo.user.dto;

public record CreateUserResponse(
        Long userId,
        String name,
        String email
) {
}
