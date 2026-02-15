package com.demo.common.exception;

public class DuplicateProductNameException extends RuntimeException {

    public DuplicateProductNameException(String name) {
        super("이미 존재하는 상품명입니다. name=" + name);
    }
}
