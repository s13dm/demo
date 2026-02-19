package com.demo.user.dto;

import com.demo.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "사용자 이름", example = "홍길동") String name,
        @Schema(description = "이메일 주소", example = "user@example.com") String email,
        @Schema(description = "사용자 권한") Role role,
        @Schema(description = "결과 메시지", example = "로그인 성공") String message
) {
}
