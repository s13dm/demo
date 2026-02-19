package com.demo.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "상품 등록 요청")
public record CreateProductRequest(
        @Schema(description = "상품명", example = "노트북") @NotBlank String name,
        @Schema(description = "상품 가격 (원, 최소 0)", example = "1500000") @Min(0) int price,
        @Schema(description = "재고 수량 (최소 0)", example = "50") @Min(0) int stock
) {
}
