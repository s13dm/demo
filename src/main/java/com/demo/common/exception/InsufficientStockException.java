package com.demo.common.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int currentStock) {
        super("재고가 부족합니다. 상품: " + productName + ", 현재 재고: " + currentStock);
    }
}
