package com.smartlogistics.common.exception;

public class ServiceUnavailableException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super("SERVICE_UNAVAILABLE", "Service distribué indisponible : " + serviceName
                + (cause != null ? " (" + cause.getMessage() + ")" : ""));
        if (cause != null) {
            initCause(cause);
        }
    }

    public ServiceUnavailableException(String message) {
        super("SERVICE_UNAVAILABLE", message);
    }
}
