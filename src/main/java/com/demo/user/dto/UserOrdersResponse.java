package com.demo.user.dto;

import java.util.List;

import com.demo.order.dto.CreateOrderResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 주문 목록 응답")
public record UserOrdersResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "사용자 이름", example = "홍길동") String userName,
        @Schema(description = "총 주문 수", example = "3") int totalOrders,
        @Schema(description = "주문 목록") List<CreateOrderResponse> orders
) {
}
