package com.demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "일반 사용자 회원가입 요청")
public record CreateUserRequest(
        @Schema(description = "사용자 이름", example = "홍길동") @NotBlank String name,
        @Schema(description = "이메일 주소", example = "user@example.com") @Email @NotBlank String email,
        @Schema(description = "비밀번호 (최소 4자)", example = "pass1234") @NotBlank @Size(min = 4) String password
) {
}
