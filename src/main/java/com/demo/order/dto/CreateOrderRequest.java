package com.demo.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @Schema(description = "주문자 사용자 ID", example = "1") @NotNull Long userId,
        @Schema(description = "주문할 상품 ID", example = "1") @NotNull Long productId,
        @Schema(description = "주문 수량 (최소 1)", example = "2") @Min(1) int quantity,
        @Schema(description = "배송 주소", example = "서울특별시 강남구 테헤란로 123") @NotBlank String shippingAddress
) {
}
