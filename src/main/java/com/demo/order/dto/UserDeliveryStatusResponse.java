package com.demo.order.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자별 배송 상태 응답")
public record UserDeliveryStatusResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "사용자 이름", example = "홍길동") String userName,
        @Schema(description = "이메일 주소", example = "user@example.com") String email,
        @Schema(description = "총 주문 수", example = "3") int totalOrders,
        @Schema(description = "배송 상태 목록") List<DeliveryStatusResponse> deliveries
) {
}
