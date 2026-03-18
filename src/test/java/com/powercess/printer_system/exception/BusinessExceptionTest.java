package com.powercess.printer_system.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("业务异常测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("应该创建带消息的业务异常")
    void shouldCreateExceptionWithMessage() {
        BusinessException exception = new BusinessException("业务错误");

        assertThat(exception.getMessage()).isEqualTo("业务错误");
        assertThat(exception.getCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("应该创建带错误码和消息的业务异常")
    void shouldCreateExceptionWithCodeAndMessage() {
        BusinessException exception = new BusinessException(404, "资源不存在");

        assertThat(exception.getMessage()).isEqualTo("资源不存在");
        assertThat(exception.getCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("应该是RuntimeException的子类")
    void shouldBeRuntimeException() {
        BusinessException exception = new BusinessException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
