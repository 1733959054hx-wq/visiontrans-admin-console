package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantOverviewVO;
import com.gzu.adminconsole.jingchen.service.MerchantOverviewService;

/**
 * 商户经营概览接口（jingchen 模块）。
 *
 * <p>类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问,与后台管理接口双向隔离。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/overview")
public class MerchantOverviewController {

    private final MerchantOverviewService service;

    public MerchantOverviewController(MerchantOverviewService service) {
        this.service = service;
    }

    /** 经营概览:指标卡 + 近 7 日趋势 + 订单摘要。 */
    @GetMapping
    public Result<MerchantOverviewVO> overview() {
        return Result.ok(service.overview());
    }
}
