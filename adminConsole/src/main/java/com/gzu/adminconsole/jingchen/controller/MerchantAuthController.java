package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantLoginRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantLoginVO;
import com.gzu.adminconsole.jingchen.service.MerchantAuthService;

/**
 * 商户认证接口（jingchen 模块）。
 *
 * <p>商户走<b>独立</b>登录流程：{@code /api/merchant/login} 由 {@code AuthInterceptor} 放行，
 * 签发商户令牌；{@code /api/merchant/logout}、{@code /api/merchant/me} 仅允许商户令牌访问。
 * 后台管理员调这些接口会因角色不符被拒绝，实现与后台管理接口双向隔离。
 *
 * <p>身份一律从 {@link AdminContext} 取当前会话，不信任任何前端传入的账号参数。
 */
@RestController
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX)
// 类级兜底：本控制器一律只服务商户令牌；未来在此新增接口时默认即收敛，避免 GET 对管理员开放。
// 注意 /login 不会因此被拦截 —— 拦截器在角色校验之前就按 isPublicPath 放行了。
@RequireRole(MerchantConstants.ROLE_CODE)
public class MerchantAuthController {

    private final MerchantAuthService service;

    public MerchantAuthController(MerchantAuthService service) {
        this.service = service;
    }

    /** 商户登录（公开，拦截器已放行 /merchant/login）。 */
    @PostMapping("/login")
    public Result<MerchantLoginVO> login(@RequestBody MerchantLoginRequest req) {
        return Result.ok(service.login(req.username(), req.password(), req.captchaId(), req.captchaClicks()));
    }

    /** 注销商户会话（仅商户令牌）。 */
    @PostMapping("/logout")
    @RequireRole(MerchantConstants.ROLE_CODE)
    public Result<Void> logout() {
        service.logout(AdminContext.get().token());
        return Result.ok(null);
    }

    /** 当前登录商户身份（仅商户令牌）。 */
    @GetMapping("/me")
    @RequireRole(MerchantConstants.ROLE_CODE)
    public Result<MerchantLoginVO.MerchantProfile> me() {
        return Result.ok(service.current(AdminContext.get()));
    }
}
