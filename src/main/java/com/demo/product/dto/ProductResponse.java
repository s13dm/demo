package com.demo.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 정보 응답")
public record ProductResponse(
        @Schema(description = "상품 ID", example = "1") Long productId,
        @Schema(description = "상품명", example = "노트북") String name,
        @Schema(description = "상품 가격 (원)", example = "1500000") int price,
        @Schema(description = "재고 수량", example = "50") int stock
) {
}
