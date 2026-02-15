package com.demo.common.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("로그인이 필요합니다.");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
