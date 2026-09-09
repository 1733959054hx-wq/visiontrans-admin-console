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
        // 参数校验前置：避免用非法请求白跑一次验证码校验（验证码校验一次即失效，会给正常用户造成无谓重试）
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("账号与口令不能为空");
        }
        // 验证码默认开启；admin-console.security.captcha-enabled=false 时可跳过（自动化联调 / 无障碍）
        if (properties.getSecurity().isCaptchaEnabled() && !captchaService.verify(captchaId, captchaClicks)) {
            throw new BusinessException("验证码校验失败，请重新验证");
        }
        String account = username.trim();
        // 风控前置校验：账号处于锁定期内直接拒绝，避免口令被暴力枚举
        requireUnlocked(account);
        // password 字段是前端用 RSA 公钥加密的密文，这里解出明文再校验
        String plainPassword = rsaKeyHolder.decrypt(password);
        AdminUserEntity user = authRepository.findByUsername(account);
        if (user == null || !PasswordHasher.matches(plainPassword, user.getPasswordHash())) {
            // 不区分"账号不存在"与"口令错误"，避免账号枚举。
            // 早期实现把 recordFailure 写在字符串拼接里，锁定阈值到时会从 concat 中直接抛异常，
            // 控制流藏在表达式求值里极难发现；这里改为显式分支。
            FailureState state = recordFailure(account);
            if (state.locked()) {
                throw new BusinessException("连续失败 " + properties.getSecurity().getLoginMaxAttempts()
                        + " 次，账号已锁定 " + properties.getSecurity().getLoginLockSeconds() + " 秒");
            }
            throw new BusinessException(state.remaining() < 0
                    ? "账号或口令不正确"
                    : "账号或口令不正确，还可尝试 " + state.remaining() + " 次");
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
                        avatarOf(user.getName())),
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

    /**
     * 记录一次登录失败并返回本次结果（是否已锁定 / 剩余可尝试次数）。
     *
     * <p>只负责记录与判定，不再在内部抛异常 —— 文案与异常一律由调用方决定。
     */
    private FailureState recordFailure(String account) {
        int max = properties.getSecurity().getLoginMaxAttempts();
        long lockSeconds = properties.getSecurity().getLoginLockSeconds();
        if (max <= 0) {
            // 未启用失败锁定
            return new FailureState(false, -1);
        }
        // compute 对同一 key 的读-改-写在 ConcurrentHashMap 上是原子的，
        // 避免并发登录请求读到同一个失败次数而绕过锁定阈值。
        // 状态含义：{ 连续失败次数, 锁定期截止时间戳(ms) }，后者为 0 表示当前未锁定。
        // 注意：只有"上一个锁定期确实存在且已过期"才重新计数；早期实现把未锁定时的 0
        // 也当作"已过期"处理，导致每次失败都从 1 重新计数，阈值永远达不到 —— 风控形同虚设。
        long[] next = attempts.compute(account, (key, state) -> {
            if (state == null) {
                return new long[] {1, 0};
            }
            boolean previousLockExpired = state[1] > 0 && state[1] <= System.currentTimeMillis();
            long base = previousLockExpired ? 0 : state[0];
            return new long[] {base + 1, state[1]};
        });
        if (next[0] >= max) {
            // 达到阈值：锁定并重置计数，锁定结束后重新计数
            attempts.put(account, new long[] {0, System.currentTimeMillis() + lockSeconds * 1000L});
            return new FailureState(true, 0);
        }
        return new FailureState(false, (int) (max - next[0]));
    }

    /** 一次登录失败后的状态：是否已触发锁定、还有几次机会（-1 表示未启用锁定）。 */
    private record FailureState(boolean locked, int remaining) {
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
        // 库里的 name / role / group 均可能为 NULL（历史数据或被手工清库），
        // 统一兜底到会话快照；早期实现直接对可能为 null 的值调 isEmpty()，会 NPE 打到全局异常处理
        String name = firstNonBlank(user != null ? user.getName() : null, admin.name());
        String role = firstNonBlank(user != null ? user.getRole() : null, admin.roleName());
        String group = firstNonBlank(user != null ? user.getGroup() : null, admin.groupName());
        return new LoginResultVO.AdminProfile(
                user != null ? user.getId() : null,
                name, role, admin.roleCode(), group,
                avatarOf(name));
    }

    /** 取第一个非空字符串；全为空时返回空串，保证后续 substring 不会 NPE。 */
    private static String firstNonBlank(String... candidates) {
        for (String value : candidates) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /** 头像占位字符：取姓名首字，空姓名回退为「管」。 */
    private static String avatarOf(String name) {
        if (name == null || name.isEmpty()) {
            return "管";
        }
        return name.substring(0, 1);
    }
}
