package com.demo.order.dto;

import java.time.LocalDateTime;

import com.demo.order.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배송 상태 응답")
public record DeliveryStatusResponse(
        @Schema(description = "주문 ID", example = "1") Long orderId,
        @Schema(description = "배송 상태") DeliveryStatus deliveryStatus,
        @Schema(description = "주문 일시") LocalDateTime orderedAt
) {
}
