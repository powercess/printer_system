package com.powercess.printer_system.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Result响应类测试")
class ResultTest {

    @Test
    @DisplayName("应该创建成功的空结果")
    void shouldCreateSuccessResult() {
        Result<Void> result = Result.success();

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.message()).isEqualTo("操作成功");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("应该创建带消息的成功结果")
    void shouldCreateSuccessResultWithMessage() {
        Result<Void> result = Result.success("操作完成");

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.message()).isEqualTo("操作完成");
    }

    @Test
    @DisplayName("应该创建带数据的成功结果")
    void shouldCreateSuccessResultWithData() {
        // 使用 Integer 类型避免与 success(String message) 方法歧义
        Result<Integer> result = Result.success(42);

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.message()).isEqualTo("操作成功");
        assertThat(result.data()).isEqualTo(42);
    }

    @Test
    @DisplayName("应该创建带消息和数据的成功结果")
    void shouldCreateSuccessResultWithMessageAndData() {
        Result<Integer> result = Result.success("查询成功", 42);

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.message()).isEqualTo("查询成功");
        assertThat(result.data()).isEqualTo(42);
    }

    @Test
    @DisplayName("应该创建错误结果")
    void shouldCreateErrorResult() {
        Result<Void> result = Result.error("服务器错误");

        assertThat(result.code()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("服务器错误");
    }

    @Test
    @DisplayName("应该创建带错误码的错误结果")
    void shouldCreateErrorResultWithCode() {
        Result<Void> result = Result.error(400, "参数错误");

        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("参数错误");
    }

    @Test
    @DisplayName("应该创建400错误结果")
    void shouldCreateBadRequestResult() {
        Result<Void> result = Result.badRequest("请求格式错误");

        assertThat(result.code()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("请求格式错误");
    }

    @Test
    @DisplayName("应该创建401错误结果")
    void shouldCreateUnauthorizedResult() {
        Result<Void> result = Result.unauthorized("未登录");

        assertThat(result.code()).isEqualTo(401);
        assertThat(result.message()).isEqualTo("未登录");
    }

    @Test
    @DisplayName("应该创建403错误结果")
    void shouldCreateForbiddenResult() {
        Result<Void> result = Result.forbidden("无权限");

        assertThat(result.code()).isEqualTo(403);
        assertThat(result.message()).isEqualTo("无权限");
    }

    @Test
    @DisplayName("应该创建404错误结果")
    void shouldCreateNotFoundResult() {
        Result<Void> result = Result.notFound("资源不存在");

        assertThat(result.code()).isEqualTo(404);
        assertThat(result.message()).isEqualTo("资源不存在");
    }
}
