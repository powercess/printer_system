package com.powercess.printer_system.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("密码工具类测试")
class PasswordUtilTest {

    @Test
    @DisplayName("应该正确哈希密码")
    void shouldHashPassword() {
        String password = "testPassword123";
        String hashed = PasswordUtil.hash(password);

        assertThat(hashed).isNotNull();
        assertThat(hashed).hasSize(64); // SHA-256 produces 64 hex characters
        assertThat(hashed).isNotEqualTo(password);
    }

    @Test
    @DisplayName("相同密码应该产生相同的哈希值")
    void shouldProduceSameHashForSamePassword() {
        String password = "mySecretPassword";
        String hash1 = PasswordUtil.hash(password);
        String hash2 = PasswordUtil.hash(password);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("不同密码应该产生不同的哈希值")
    void shouldProduceDifferentHashForDifferentPassword() {
        String hash1 = PasswordUtil.hash("password1");
        String hash2 = PasswordUtil.hash("password2");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("应该正确验证密码")
    void shouldVerifyPasswordCorrectly() {
        String password = "correctPassword";
        String hashed = PasswordUtil.hash(password);

        assertThat(PasswordUtil.verify(password, hashed)).isTrue();
        assertThat(PasswordUtil.verify("wrongPassword", hashed)).isFalse();
    }

    @Test
    @DisplayName("验证应该忽略大小写")
    void shouldVerifyCaseInsensitive() {
        String password = "TestPassword";
        String hashed = PasswordUtil.hash(password).toUpperCase();

        assertThat(PasswordUtil.verify(password, hashed)).isTrue();
    }

    @Test
    @DisplayName("空密码应该能被哈希")
    void shouldHashEmptyPassword() {
        String hashed = PasswordUtil.hash("");
        assertThat(hashed).isNotNull();
        assertThat(hashed).hasSize(64);
    }
}
