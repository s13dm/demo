package com.demo.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 4) String password,
        @NotBlank String adminSecretKey
) {
}
