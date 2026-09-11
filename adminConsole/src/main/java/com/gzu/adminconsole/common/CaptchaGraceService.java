package com.gzu.adminconsole.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * 登录免验证码宽限（公共登录设施，管理员与商户两条登录链路共用）。
 *
 * <p>成功登录过的账号在短时间窗口内再次登录时跳过点击式验证码：
 * 验证码开启（{@code admin-console.security.captcha-enabled=true}）是默认安全策略，
 * 但演示 / 反复进出工作台时每回都要点三个字很繁琐；这里按「身份 + 账号」记录最近一次
 * 成功登录的时间，宽限期内放行。会话同为内存态，重启即清零，行为与现有会话机制一致。
 *
 * <p>未在宽限期内且未携带验证码的登录请求，按 {@link #CAPTCHA_REQUIRED_CODE} 拒绝，
 * 前端据此弹出验证码面板重新提交，而不是把"需要验证码"当成普通报错展示。
 */
@Service
public class CaptchaGraceService {

    /** 需要验证码的专用业务码（HTTP 层仍为 400，前端按该 code 弹出验证码面板）。 */
    public static final int CAPTCHA_REQUIRED_CODE = 460;

    /** 免验证码宽限窗口：10 分钟。 */
    private static final long GRACE_MILLIS = 10 * 60 * 1000L;

    /** 管理员登录身份域。 */
    public static final String SCOPE_ADMIN = "admin";

    /** 商户登录身份域。 */
    public static final String SCOPE_MERCHANT = "merchant";

    /** key = 身份域 + ':' + 账号 → 宽限期截止时间戳(ms)。 */
    private final Map<String, Long> trustedUntil = new ConcurrentHashMap<>();

    /** 账号是否处于免验证码宽限期内。 */
    public boolean isTrusted(String scope, String account) {
        Long until = trustedUntil.get(key(scope, account));
        return until != null && until > System.currentTimeMillis();
    }

    /** 登录成功后记入宽限期；顺手清理过期项，避免长期运行下的缓慢膨胀。 */
    public void grant(String scope, String account) {
        trustedUntil.put(key(scope, account), System.currentTimeMillis() + GRACE_MILLIS);
        long now = System.currentTimeMillis();
        trustedUntil.values().removeIf(until -> until <= now);
    }

    private static String key(String scope, String account) {
        return (scope == null ? "" : scope) + ':' + (account == null ? "" : account.trim());
    }
}
