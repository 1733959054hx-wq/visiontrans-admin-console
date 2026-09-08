package com.gzu.adminconsole.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.RsaKeyHolder;
import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.AuthInterceptor;
import com.gzu.adminconsole.dto.meta.LoginResultVO;
import com.gzu.adminconsole.service.AuthService;
import com.gzu.adminconsole.service.CaptchaService;

import java.util.List;

/**
 * 登录鉴权接口（无需令牌即可访问）。
 */
@RestController
@RequestMapping("${admin-console.api.base-path:/api}/auth")
public class AuthController {

    private final AuthService service;
    private final CaptchaService captchaService;
    private final RsaKeyHolder rsaKeyHolder;

    public AuthController(AuthService service, CaptchaService captchaService, RsaKeyHolder rsaKeyHolder) {
        this.service = service;
        this.captchaService = captchaService;
        this.rsaKeyHolder = rsaKeyHolder;
    }

    /** 登录。 */
    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody LoginRequest request) {
        if (request == null) {
            return Result.fail(400, "请求体不能为空");
        }
        return Result.ok(service.login(request.username(), request.password(),
                request.captchaId(), request.captchaClicks()));
    }

    /** 生成点击式图形验证码。 */
    @GetMapping("/captcha")
    public Result<CaptchaService.CaptchaChallenge> captcha() {
        return Result.ok(captchaService.create());
    }

    /** 下发 RSA 公钥（前端用于加密登录口令）。 */
    @GetMapping("/public-key")
    public Result<String> publicKey() {
        return Result.ok(rsaKeyHolder.publicKeyBase64());
    }

    /** 登出。 */
    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        service.logout(request.getHeader(AuthInterceptor.TOKEN_HEADER));
        return Result.ok(true);
    }

    /** 当前登录管理员。 */
    @GetMapping("/me")
    public Result<LoginResultVO.AdminProfile> me() {
        return Result.ok(service.current());
    }

    /**
     * 登录请求。
     *
     * @param username      登录账号
     * @param password      RSA 公钥加密后的口令密文
     * @param captchaId     验证码挑战 ID
     * @param captchaClicks 用户按顺序点击的坐标
     */
    public record LoginRequest(String username, String password, String captchaId,
                               List<CaptchaService.Point> captchaClicks) {
    }
}
