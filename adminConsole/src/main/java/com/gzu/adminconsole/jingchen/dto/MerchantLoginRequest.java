package com.gzu.adminconsole.jingchen.dto;

import java.util.List;

import com.gzu.adminconsole.service.CaptchaService;

/**
 * 商户登录请求（模块自有）。
 *
 * @param username      登录账号（即商户编码）
 * @param password      RSA 加密后的口令密文
 * @param captchaId     点击式验证码挑战 ID（与主工程共用验证码设施）
 * @param captchaClicks 按顺序点击的坐标
 */
public record MerchantLoginRequest(String username, String password, String captchaId,
                                   List<CaptchaService.Point> captchaClicks) {
}
