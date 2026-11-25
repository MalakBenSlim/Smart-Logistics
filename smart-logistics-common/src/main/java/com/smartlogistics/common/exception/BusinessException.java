package com.smartlogistics.common.exception;

public class BusinessException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        this("BUSINESS_ERROR", message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
