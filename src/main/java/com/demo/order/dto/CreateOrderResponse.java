package com.demo.order.dto;

import java.time.LocalDateTime;

import com.demo.order.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 생성 응답")
public record CreateOrderResponse(
        @Schema(description = "주문 ID", example = "1") Long orderId,
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "상품명", example = "노트북") String productName,
        @Schema(description = "주문 수량", example = "2") int quantity,
        @Schema(description = "배송 주소", example = "서울특별시 강남구 테헤란로 123") String shippingAddress,
        @Schema(description = "배송 상태") DeliveryStatus deliveryStatus,
        @Schema(description = "주문 일시") LocalDateTime orderedAt
) {
}
