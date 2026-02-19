package com.demo.order.dto;

import com.demo.order.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "배송 상태 업데이트 요청")
public record UpdateDeliveryStatusRequest(
        @Schema(description = "변경할 배송 상태 (ORDERED, PREPARING, SHIPPED, DELIVERED, CANCELLED)", example = "SHIPPED") @NotNull DeliveryStatus deliveryStatus
) {
}
