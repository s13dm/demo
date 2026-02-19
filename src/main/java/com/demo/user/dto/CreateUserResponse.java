package com.demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 회원가입 응답")
public record CreateUserResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "사용자 이름", example = "홍길동") String name,
        @Schema(description = "이메일 주소", example = "user@example.com") String email
) {
}
