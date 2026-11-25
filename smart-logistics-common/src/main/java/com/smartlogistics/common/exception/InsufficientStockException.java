package com.smartlogistics.common.exception;

public class InsufficientStockException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String message) {
        super("INSUFFICIENT_STOCK", message);
    }
}
