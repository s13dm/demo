package com.demo.user.dto;

import com.demo.user.entity.Role;

public record LoginResponse(
        Long userId,
        String name,
        String email,
        Role role,
        String message
) {
}
