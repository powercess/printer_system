package com.powercess.printer_system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(
    int code,
    String message,
    T data
) {
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
}