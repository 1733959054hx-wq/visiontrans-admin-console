package com.gzu.adminconsole.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.dto.meta.LoginResultVO;
import com.gzu.adminconsole.dto.meta.UnifiedLoginVO;
import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.jingchen.dto.MerchantLoginVO;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantRepository;
import com.gzu.adminconsole.jingchen.service.MerchantAuthService;
import com.gzu.adminconsole.repository.AuthRepository;

/**
 * 统一登录：用户不再选择身份，后端按登录账号自动识别管理员 / 商户。
 *
 * <p>识别规则（<b>管理员优先</b>）：
 * <ol>
 *   <li>账号在管理员表中存在 → 走管理员登录链（验证码、风控、会话签发全部沿用 {@link AuthService}）；</li>
 *   <li>否则账号在商户表中存在 → 走商户登录链（{@link MerchantAuthService}，jingchen 包零改动）；</li>
 *   <li>两边都不存在 → 仍交给管理员登录链处理，得到与原后台登录一致的
 *       "账号或口令不正确"文案与失败锁定行为，避免账号枚举。</li>
 * </ol>
 *
 * <p>先探测账号归属、再执行各自校验链，保证商户账号的失败尝试不会污染管理员的风控计数，
 * 反之亦然；两套令牌体系、会话表、验证码宽限 scope 均保持不变。
 */
@Service
public class UnifiedAuthService {

    /** 统一入口的商户会话设备标识：与前端商户端约定一致（web / android / ios）。 */
    private static final String DEVICE_WEB = "web";

    private final AuthRepository authRepository;
    private final MerchantRepository merchantRepository;
    private final AuthService authService;
    private final MerchantAuthService merchantAuthService;

    public UnifiedAuthService(AuthRepository authRepository, MerchantRepository merchantRepository,
                              AuthService authService, MerchantAuthService merchantAuthService) {
        this.authRepository = authRepository;
        this.merchantRepository = merchantRepository;
        this.authService = authService;
        this.merchantAuthService = merchantAuthService;
    }

    /**
     * 统一登录。
     *
     * @param username      登录账号（管理员用户名或商户编码）
     * @param password      RSA 公钥加密后的口令密文
     * @param captchaId     验证码挑战 ID
     * @param captchaClicks 用户按顺序点击的坐标
     */
    @Transactional
    public UnifiedLoginVO login(String username, String password, String captchaId,
                                List<CaptchaService.Point> captchaClicks) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("账号与口令不能为空");
        }
        String account = username.trim();

        // 1) 管理员优先：账号命中管理员表即走管理员登录链（含停用校验、弱哈希升级等全部既有逻辑）
        AdminUserEntity admin = authRepository.findByUsername(account);
        if (admin != null) {
            LoginResultVO result = authService.login(account, password, captchaId, captchaClicks);
            return UnifiedLoginVO.ofAdmin(account, result);
        }

        // 2) 商户：账号命中商户编码表即走商户登录链，令牌写入商户会话表
        MerchantEntity merchant = merchantRepository.findByCode(account);
        if (merchant != null) {
            MerchantLoginVO result = merchantAuthService.login(account, password, captchaId,
                    captchaClicks, DEVICE_WEB);
            return UnifiedLoginVO.ofMerchant(result);
        }

        // 3) 两边都不存在：复用管理员登录链的失败处理（统一错误文案 + 失败锁定，不暴露账号归属）
        LoginResultVO result = authService.login(account, password, captchaId, captchaClicks);
        return UnifiedLoginVO.ofAdmin(account, result);
    }
}
