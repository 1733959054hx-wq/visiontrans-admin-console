package com.gzu.adminconsole.jingchen.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.PasswordHasher;
import com.gzu.adminconsole.common.RsaKeyHolder;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantLoginVO;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantSessionEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantSessionRepository;
import com.gzu.adminconsole.service.CaptchaService;

/**
 * 商户登录与身份服务（模块自有，不复用后台 AuthService）。
 *
 * <p>商户账号、口令、会话全部由本模块维护；仅复用主工程的「加密基础设施」
 * （RSA 公钥解密、点击式验证码、口令哈希校验），这些是公共工具，不含任何业务数据。
 */
@Service
public class MerchantAuthService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MerchantRepository repository;
    private final MerchantSessionRepository sessions;
    private final CaptchaService captchaService;
    private final RsaKeyHolder rsaKeyHolder;
    private final AppProperties properties;

    public MerchantAuthService(MerchantRepository repository, MerchantSessionRepository sessions,
                              CaptchaService captchaService, RsaKeyHolder rsaKeyHolder, AppProperties properties) {
        this.repository = repository;
        this.sessions = sessions;
        this.captchaService = captchaService;
        this.rsaKeyHolder = rsaKeyHolder;
        this.properties = properties;
    }

    /**
     * 商户登录：验证码 → RSA 解密 → 凭据比对 → 签发独立商户令牌。
     */
    public MerchantLoginVO login(String username, String password, String captchaId,
                                List<CaptchaService.Point> clicks) {
        if (properties.getSecurity().isCaptchaEnabled() && !captchaService.verify(captchaId, clicks)) {
            throw new BusinessException("验证码校验失败，请重新验证");
        }
        String raw = rsaKeyHolder.decrypt(password);
        MerchantEntity m = repository.findByCode(username);
        if (m == null || m.getPasswordHash() == null || !PasswordHasher.matches(raw, m.getPasswordHash())) {
            throw new BusinessException("账号或口令错误");
        }
        m.setLastLogin(LocalDateTime.now().format(FMT));
        MerchantSessionEntity s = sessions.create(m);
        return new MerchantLoginVO(s.getToken(), profileOf(m), s.getExpireAt().format(FMT));
    }

    /** 注销当前商户会话。 */
    public void logout(String token) {
        sessions.delete(token);
    }

    /** 返回当前登录商户的身份档案（用于 /me）。 */
    public MerchantLoginVO.MerchantProfile current(AdminContext.CurrentAdmin admin) {
        return new MerchantLoginVO.MerchantProfile(admin.username(), admin.name(), admin.roleName(),
                admin.roleCode(), avatarOf(admin.name()));
    }

    private MerchantLoginVO.MerchantProfile profileOf(MerchantEntity m) {
        return new MerchantLoginVO.MerchantProfile(m.getCode(), m.getName(), MerchantConstants.ROLE_NAME,
                MerchantConstants.ROLE_CODE, avatarOf(m.getName()));
    }

    private static String avatarOf(String name) {
        return (name == null || name.isEmpty()) ? "商" : name.substring(0, 1);
    }
}
