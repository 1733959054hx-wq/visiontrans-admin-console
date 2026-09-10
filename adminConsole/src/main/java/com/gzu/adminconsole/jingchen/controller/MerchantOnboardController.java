package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantOnboardRequest;
import com.gzu.adminconsole.jingchen.service.MerchantOnboardService;

/**
 * 商户入驻接口（jingchen 模块,商户侧:资质提交 / 合同签署 / 状态查询）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/onboarding")
public class MerchantOnboardController {

    private final MerchantOnboardService service;

    public MerchantOnboardController(MerchantOnboardService service) {
        this.service = service;
    }

    /** 当前商户的入驻申请(从未提交时返回 null)。 */
    @GetMapping
    public Result<Object> mine() {
        return Result.ok(service.mine());
    }

    /** 提交资质(首次申请 / 驳回后重新提交)。 */
    @PostMapping("/submit")
    public Result<Object> submit(@RequestBody MerchantOnboardRequest req) {
        return Result.ok(service.submit(req));
    }

    /** 签署合作协议。 */
    @PostMapping("/contract/sign")
    public Result<Object> signContract() {
        return Result.ok(service.signContract());
    }
}
