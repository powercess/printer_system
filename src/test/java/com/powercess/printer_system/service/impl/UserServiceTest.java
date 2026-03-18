package com.powercess.printer_system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.user.*;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.entity.UserGroup;
import com.powercess.printer_system.entity.WalletTransaction;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.UserGroupMapper;
import com.powercess.printer_system.mapper.UserMapper;
import com.powercess.printer_system.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserGroupMapper userGroupMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTests {

        @Test
        @DisplayName("应该成功注册新用户")
        void shouldRegisterNewUser() {
            UserRegisterRequest request = new UserRegisterRequest("testuser", "password123", "test@example.com");

            when(userMapper.findByUsername("testuser")).thenReturn(Optional.empty());
            when(userMapper.findByEmail("test@example.com")).thenReturn(Optional.empty());
            when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return 1;
            });

            assertThatCode(() -> userService.register(request)).doesNotThrowAnyException();

            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在时应该抛出异常")
        void shouldThrowExceptionWhenUsernameExists() {
            UserRegisterRequest request = new UserRegisterRequest("existinguser", "password123", "test@example.com");

            when(userMapper.findByUsername("existinguser")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在")
                .extracting("code").isEqualTo(400);
        }

        @Test
        @DisplayName("邮箱已被注册时应该抛出异常")
        void shouldThrowExceptionWhenEmailExists() {
            UserRegisterRequest request = new UserRegisterRequest("newuser", "password123", "existing@example.com");

            when(userMapper.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userMapper.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被注册")
                .extracting("code").isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {

        @Test
        @DisplayName("应该成功登录并返回token")
        void shouldLoginSuccessfully() {
            UserLoginRequest request = new UserLoginRequest("testuser", "password123");

            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPasswordHash(org.springframework.util.DigestUtils.md5DigestAsHex("password123".getBytes()));

            // 使用 PasswordUtil 的实际哈希
            user.setPasswordHash(com.powercess.printer_system.utils.PasswordUtil.hash("password123"));

            when(userMapper.findByUsername("testuser")).thenReturn(Optional.of(user));
            stpUtilMock.when(() -> StpUtil.login(1L)).then(invocation -> null);
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn("test-token");

            String token = userService.login(request);

            assertThat(token).isEqualTo("test-token");
            stpUtilMock.verify(() -> StpUtil.login(1L));
        }

        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            UserLoginRequest request = new UserLoginRequest("nonexistent", "password");

            when(userMapper.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在")
                .extracting("code").isEqualTo(401);
        }

        @Test
        @DisplayName("密码错误时应该抛出异常")
        void shouldThrowExceptionWhenPasswordWrong() {
            UserLoginRequest request = new UserLoginRequest("testuser", "wrongpassword");

            User user = new User();
            user.setId(1L);
            user.setPasswordHash(com.powercess.printer_system.utils.PasswordUtil.hash("correctpassword"));

            when(userMapper.findByUsername("testuser")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误")
                .extracting("code").isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("获取用户信息测试")
    class GetProfileTests {

        @Test
        @DisplayName("应该成功获取用户信息")
        void shouldGetUserProfile() {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setEmail("test@example.com");
            user.setGroupId(1L);

            UserGroup group = new UserGroup();
            group.setGroupName("普通用户");

            when(userMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(user));
            when(userGroupMapper.selectById(1L)).thenReturn(group);

            User result = userService.getProfile(1L);

            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getGroupName()).isEqualTo("普通用户");
        }

        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userMapper.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
        }
    }

    @Nested
    @DisplayName("钱包余额测试")
    class WalletBalanceTests {

        @Test
        @DisplayName("应该正确获取钱包余额")
        void shouldGetWalletBalance() {
            User user = new User();
            user.setId(1L);
            user.setWalletBalance(new BigDecimal("100.50"));

            when(userMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(user));

            BigDecimal balance = userService.getWalletBalance(1L);

            assertThat(balance).isEqualByComparingTo(new BigDecimal("100.50"));
        }
    }

    @Nested
    @DisplayName("钱包充值测试")
    class RechargeTests {

        @Test
        @DisplayName("应该成功充值")
        void shouldRechargeSuccessfully() {
            User user = new User();
            user.setId(1L);
            user.setWalletBalance(new BigDecimal("50.00"));

            WalletRechargeRequest request = new WalletRechargeRequest(new BigDecimal("30.00"), "alipay");

            when(userMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(user));
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

            assertThatCode(() -> userService.recharge(1L, request)).doesNotThrowAnyException();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).updateById(userCaptor.capture());
            assertThat(userCaptor.getValue().getWalletBalance()).isEqualByComparingTo(new BigDecimal("80.00"));

            ArgumentCaptor<WalletTransaction> transactionCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
            verify(walletTransactionMapper).insert(transactionCaptor.capture());
            WalletTransaction capturedTransaction = transactionCaptor.getValue();
            assertThat(capturedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(capturedTransaction.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(capturedTransaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("80.00"));
        }
    }

    @Nested
    @DisplayName("更新用户资料测试")
    class UpdateProfileTests {

        @Test
        @DisplayName("应该成功更新用户昵称")
        void shouldUpdateNickname() {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setEmail("test@example.com");

            UserProfileUpdateRequest request = new UserProfileUpdateRequest("新昵称", null, null);

            when(userMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(user));
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            assertThatCode(() -> userService.updateProfile(1L, request)).doesNotThrowAnyException();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).updateById(userCaptor.capture());
            assertThat(userCaptor.getValue().getNickname()).isEqualTo("新昵称");
        }

        @Test
        @DisplayName("更新邮箱时应该检查唯一性")
        void shouldCheckEmailUniqueness() {
            User user = new User();
            user.setId(1L);
            user.setEmail("old@example.com");

            UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, "new@example.com");

            when(userMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(user));
            when(userMapper.findByEmail("new@example.com")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被使用");
        }
    }
}