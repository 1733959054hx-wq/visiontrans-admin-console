package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.PasswordHasher;
import com.gzu.adminconsole.common.RsaKeyHolder;
import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.entity.AuthSessionEntity;
import com.gzu.adminconsole.repository.AuthRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录流程单元测试：守住「参数校验前置」与「失败锁定」两处容易在重构中回归的行为。
 *
 * <p>刻意不用 Spring 上下文 —— 依赖全部 Mock，秒级完成，也不需要数据库。
 */
class AuthServiceLoginFlowTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "Admin#2026";
    private static final int MAX_ATTEMPTS = 3;

    private AuthRepository authRepository;
    private CaptchaService captchaService;
    private RsaKeyHolder rsaKeyHolder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        authRepository = mock(AuthRepository.class);
        captchaService = mock(CaptchaService.class);
        rsaKeyHolder = mock(RsaKeyHolder.class);

        AppProperties.Security security = new AppProperties.Security();
        security.setLoginMaxAttempts(MAX_ATTEMPTS);
        security.setLoginLockSeconds(300);
        AppProperties properties = new AppProperties();
        properties.setSecurity(security);

        service = new AuthService(authRepository, captchaService, rsaKeyHolder, properties);

        AdminUserEntity user = userWith(PasswordHasher.hash(PASSWORD));

        when(captchaService.verify(any(), any())).thenReturn(true);
        when(rsaKeyHolder.decrypt(anyString())).thenReturn(PASSWORD);
        when(authRepository.findByUsername(USERNAME)).thenReturn(user);
        when(authRepository.createSession(any(AdminUserEntity.class))).thenAnswer(this::session);
    }

    /** 空账号不应触发验证码校验：校验一次即失效，白跑会让用户莫名重试。 */
    @Test
    void blankCredentialsAreRejectedBeforeCaptchaVerification() {
        assertThrows(BusinessException.class,
                () -> service.login("", "cipher", "cid", List.of()));
        verify(captchaService, never()).verify(any(), any());
    }

    /** 连续失败达到阈值后应触发锁定，且锁定期间不再走口令校验。 */
    @Test
    void repeatedFailuresLockTheAccount() {
        when(rsaKeyHolder.decrypt(anyString())).thenReturn("wrong-password");

        for (int i = 1; i < MAX_ATTEMPTS; i++) {
            final int attempt = i;
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.login(USERNAME, "cipher", "cid", List.of()));
            assertTrue(ex.getMessage().contains("还可尝试"),
                    () -> "第 " + attempt + " 次失败应提示剩余次数，实际：" + ex.getMessage());
        }

        BusinessException locked = assertThrows(BusinessException.class,
                () -> service.login(USERNAME, "cipher", "cid", List.of()));
        assertTrue(locked.getMessage().contains("账号已锁定"),
                () -> "达到阈值应提示锁定，实际：" + locked.getMessage());

        // 锁定期间即使用正确口令也应被风控前置拦截（不再透库校验）
        when(rsaKeyHolder.decrypt(anyString())).thenReturn(PASSWORD);
        BusinessException stillLocked = assertThrows(BusinessException.class,
                () -> service.login(USERNAME, "cipher", "cid", List.of()));
        assertTrue(stillLocked.getMessage().contains("临时锁定"),
                () -> "锁定期间应被风控拦截，实际：" + stillLocked.getMessage());
        verify(authRepository, never()).createSession(any(AdminUserEntity.class));
    }

    /** 口令错误时不得写入任何哈希（避免攻击者用登录接口改写凭据）。 */
    @Test
    void failedLoginNeverRewritesHash() {
        when(rsaKeyHolder.decrypt(anyString())).thenReturn("wrong-password");

        assertThrows(BusinessException.class, () -> service.login(USERNAME, "cipher", "cid", List.of()));
        verify(authRepository, never()).updatePasswordHash(anyString(), anyString());
    }

    /** 历史 SHA-256 弱密文在登录成功后应被静默升级为 PBKDF2（无感知迁移链路）。 */
    @Test
    void successfulLoginUpgradesOutdatedHash() {
        when(authRepository.findByUsername(USERNAME)).thenReturn(userWith(legacyHashOf(PASSWORD)));

        service.login(USERNAME, "cipher", "cid", List.of());

        verify(authRepository, times(1)).updatePasswordHash(eq(USERNAME), anyString());
    }

    /** 复刻 PasswordHasher 的历史格式：HEX(SHA-256("visiontrans-admin" + 口令))。 */
    private static String legacyHashOf(String rawPassword) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(("visiontrans-admin" + rawPassword).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** 构造一个启用状态的管理员账号。 */
    private static AdminUserEntity userWith(String passwordHash) {
        return new AdminUserEntity("系统管理员", "超级管理员", "平台运维组", "133****0000", "启用",
                "2026-09-09 10:00", USERNAME, passwordHash);
    }

    @Test
    void successfulLoginCreatesSession() {
        service.login(USERNAME, "cipher", "cid", List.of());
        verify(authRepository, times(1)).createSession(any(AdminUserEntity.class));
    }

    /** createSession 的桩：返回一条 8 小时后过期的会话。 */
    private AuthSessionEntity session(InvocationOnMock invocation) {
        AdminUserEntity user = invocation.getArgument(0);
        return new AuthSessionEntity("token-" + user.getUsername(), user.getUsername(), user.getName(),
                "SUPER_ADMIN", user.getRole(), user.getGroup(), LocalDateTime.now().plusHours(8));
    }
}
