package com.powercess.printer_system.cups;

/**
 * CUPS 操作异常
 */
public class CupsException extends Exception {

    private final ErrorCode errorCode;

    public CupsException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN;
    }

    public CupsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN;
    }

    public CupsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CupsException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        CONNECTION_FAILED,
        PRINTER_NOT_FOUND,
        PRINT_FAILED,
        JOB_NOT_FOUND,
        CANCEL_FAILED,
        INVALID_OPTIONS,
        AUTHENTICATION_FAILED,
        TIMEOUT,
        UNKNOWN
    }
}