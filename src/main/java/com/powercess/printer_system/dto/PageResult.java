package com.powercess.printer_system.dto;

import java.util.List;

public record PageResult<T>(
    long total,
    int page,
    int pageSize,
    List<T> items
) {
    public static <T> PageResult<T> of(long total, int page, int pageSize, List<T> items) {
        return new PageResult<>(total, page, pageSize, items);
    }
}