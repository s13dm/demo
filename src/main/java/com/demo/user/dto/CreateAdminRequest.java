package com.demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 회원가입 요청")
public record CreateAdminRequest(
        @Schema(description = "관리자 이름", example = "관리자") @NotBlank String name,
        @Schema(description = "이메일 주소", example = "admin@example.com") @Email @NotBlank String email,
        @Schema(description = "비밀번호 (최소 4자)", example = "admin1234") @NotBlank @Size(min = 4) String password,
        @Schema(description = "관리자 시크릿 키", example = "ADMIN_SECRET") @NotBlank String adminSecretKey
) {
}
