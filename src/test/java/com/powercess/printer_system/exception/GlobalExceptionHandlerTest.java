package com.powercess.printer_system.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.powercess.printer_system.dto.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("全局异常处理器测试")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("应该处理BusinessException")
    void shouldHandleBusinessException() {
        BusinessException exception = new BusinessException(400, "用户名已存在");

        Result<Void> result = handler.handleBusinessException(exception);

        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("用户名已存在");
    }

    @Test
    @DisplayName("应该处理NotLoginException")
    void shouldHandleNotLoginException() {
        NotLoginException exception = mock(NotLoginException.class);
        when(exception.getMessage()).thenReturn("Not logged in");

        Result<Void> result = handler.handleNotLoginException(exception);

        assertThat(result.code()).isEqualTo(401);
        assertThat(result.message()).isEqualTo("请先登录");
    }

    @Test
    @DisplayName("应该处理NotRoleException")
    void shouldHandleNotRoleException() {
        NotRoleException exception = mock(NotRoleException.class);
        when(exception.getMessage()).thenReturn("No role");

        Result<Void> result = handler.handleNotRoleException(exception);

        assertThat(result.code()).isEqualTo(403);
        assertThat(result.message()).isEqualTo("权限不足");
    }

    @Test
    @DisplayName("应该处理NotPermissionException")
    void shouldHandleNotPermissionException() {
        NotPermissionException exception = mock(NotPermissionException.class);
        when(exception.getMessage()).thenReturn("No permission");

        Result<Void> result = handler.handleNotPermissionException(exception);

        assertThat(result.code()).isEqualTo(403);
        assertThat(result.message()).isEqualTo("权限不足");
    }

    @Test
    @DisplayName("应该处理MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "username", "不能为空");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        var result = handler.handleValidationException(exception);

        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("参数验证失败");
        assertThat(result.data()).containsEntry("username", "不能为空");
    }

    @Test
    @DisplayName("应该处理ConstraintViolationException")
    void shouldHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("必须为正数");

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);

        ConstraintViolationException exception = new ConstraintViolationException(
            "validation failed",
            violations
        );

        Result<Void> result = handler.handleConstraintViolationException(exception);

        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).contains("必须为正数");
    }

    @Test
    @DisplayName("应该处理通用Exception")
    void shouldHandleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");

        Result<Void> result = handler.handleException(exception);

        assertThat(result.code()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("服务器内部错误");
    }
}