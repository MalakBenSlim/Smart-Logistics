package com.smartlogistics.common.exception;

public class AuthException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super("AUTH_ERROR", message);
    }

    public AuthException(String errorCode, String message) {
        super(errorCode, message);
    }
}
