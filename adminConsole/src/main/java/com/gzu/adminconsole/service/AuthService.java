package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.common.PasswordHasher;
import com.gzu.adminconsole.common.RsaKeyHolder;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.dto.meta.LoginResultVO;
import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.entity.AuthSessionEntity;
import com.gzu.adminconsole.repository.AuthRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录 / 登出 / 当前管理员。
 */
@Service
public class AuthService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthRepository authRepository;
    private final CaptchaService captchaService;
    private final RsaKeyHolder rsaKeyHolder;
    private final AppProperties properties;

    /**
     * 登录失败风控：账号 -> { 连续失败次数, 锁定期截止时间戳(ms) }。
     * 仅统计"通过验证码后的口令校验失败"，避免用户点错验证码被误锁。
     */
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    public AuthService(AuthRepository authRepository, CaptchaService captchaService, RsaKeyHolder rsaKeyHolder,
                       AppProperties properties) {
        this.authRepository = authRepository;
        this.captchaService = captchaService;
        this.rsaKeyHolder = rsaKeyHolder;
        this.properties = properties;
    }

    /** 登录：先校验点击式验证码，再解密 RSA 口令并校验账号。 */
    @Transactional
    public LoginResultVO login(String username, String password, String captchaId,
                               List<CaptchaService.Point> captchaClicks) {
        // 验证码默认开启；admin-console.security.captcha-enabled=false 时可跳过（自动化联调 / 无障碍）
        if (properties.getSecurity().isCaptchaEnabled() && !captchaService.verify(captchaId, captchaClicks)) {
            throw new BusinessException("验证码校验失败，请重新验证");
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("账号与口令不能为空");
        }
        String account = username.trim();
        // 风控前置校验：账号处于锁定期内直接拒绝，避免口令被暴力枚举
        requireUnlocked(account);
        // password 字段是前端用 RSA 公钥加密的密文，这里解出明文再校验
        String plainPassword = rsaKeyHolder.decrypt(password);
        AdminUserEntity user = authRepository.findByUsername(account);
        if (user == null || !PasswordHasher.matches(plainPassword, user.getPasswordHash())) {
            // 不区分"账号不存在"与"口令错误"，避免账号枚举
            throw new BusinessException("账号或口令不正确，" + recordFailure(account));
        }
        // 历史弱哈希（SHA-256 固定盐 / 迭代次数过旧）在本次登录成功后静默升级
        if (PasswordHasher.needsRehash(user.getPasswordHash())) {
            authRepository.updatePasswordHash(account, PasswordHasher.hash(plainPassword));
        }
        if (!"启用".equals(user.getStatus())) {
            throw new BusinessException("该账号已停用，请联系超级管理员");
        }
        // 校验通过：清空该账号的失败记录
        attempts.remove(account);

        user.setLastLogin(LocalDateTime.now().format(FORMATTER));
        AuthSessionEntity session = authRepository.createSession(user);

        return new LoginResultVO(
                session.getToken(),
                new LoginResultVO.AdminProfile(
                        user.getId(),
                        user.getName(),
                        user.getRole(),
                        session.getRoleCode(),
                        user.getGroup(),
                        user.getName().isEmpty() ? "管" : user.getName().substring(0, 1)),
                session.getExpireAt().format(FORMATTER));
    }

    /** 风控校验：账号处于锁定期内直接拒绝。 */
    private void requireUnlocked(String account) {
        long[] state = attempts.get(account);
        if (state == null || state[1] <= System.currentTimeMillis()) {
            return;
        }
        long seconds = (state[1] - System.currentTimeMillis()) / 1000 + 1;
        throw new BusinessException("连续失败次数过多，账号已临时锁定，请 " + seconds + " 秒后重试");
    }

    /** 记录一次失败并返回剩余次数提示；达到阈值则立即锁定。 */
    private String recordFailure(String account) {
        int max = properties.getSecurity().getLoginMaxAttempts();
        long lockSeconds = properties.getSecurity().getLoginLockSeconds();
        if (max <= 0) {
            return "请重新输入";
        }
        // compute 对同一 key 的读-改-写在 ConcurrentHashMap 上是原子的，
        // 避免并发登录请求读到同一个失败次数而绕过锁定阈值
        long[] next = attempts.compute(account, (key, state) -> {
            long base = (state == null || state[1] <= System.currentTimeMillis()) ? 0 : state[0];
            return new long[] {base + 1, 0};
        });
        if (next[0] >= max) {
            // 达到阈值：锁定并重置计数，锁定结束后重新计数
            attempts.put(account, new long[] {0, System.currentTimeMillis() + lockSeconds * 1000L});
            throw new BusinessException("连续失败 " + max + " 次，账号已锁定 " + lockSeconds + " 秒");
        }
        return "还可尝试 " + (max - next[0]) + " 次";
    }

    /** 登出：删除会话。 */
    @Transactional
    public void logout(String token) {
        authRepository.deleteSession(token);
    }

    /** 当前登录管理员（令牌无效时抛 401）。资料实时读库，改名 / 换组后旧令牌也能读到最新信息。 */
    public LoginResultVO.AdminProfile current() {
        AdminContext.CurrentAdmin admin = AdminContext.get();
        if (admin == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        AdminUserEntity user = authRepository.findByUsername(admin.username());
        String name = user != null ? user.getName() : admin.name();
        String role = user != null ? user.getRole() : admin.roleName();
        String group = user != null ? user.getGroup() : admin.groupName();
        return new LoginResultVO.AdminProfile(
                user != null ? user.getId() : null,
                name, role, admin.roleCode(), group,
                name.isEmpty() ? "管" : name.substring(0, 1));
    }
}
