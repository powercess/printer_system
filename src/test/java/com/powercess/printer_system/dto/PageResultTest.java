package com.powercess.printer_system.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult分页结果测试")
class PageResultTest {

    @Test
    @DisplayName("应该创建分页结果")
    void shouldCreatePageResult() {
        List<String> items = List.of("item1", "item2", "item3");

        PageResult<String> result = PageResult.of(100, 1, 10, items);

        assertThat(result.total()).isEqualTo(100);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.items()).hasSize(3);
        assertThat(result.items()).containsExactly("item1", "item2", "item3");
    }

    @Test
    @DisplayName("应该创建空分页结果")
    void shouldCreateEmptyPageResult() {
        PageResult<Object> result = PageResult.of(0, 1, 10, List.of());

        assertThat(result.total()).isZero();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("应该支持泛型类型")
    void shouldSupportGenericTypes() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        PageResult<Integer> result = PageResult.of(50, 2, 10, numbers);

        assertThat(result.items()).containsExactly(1, 2, 3, 4, 5);
    }
}