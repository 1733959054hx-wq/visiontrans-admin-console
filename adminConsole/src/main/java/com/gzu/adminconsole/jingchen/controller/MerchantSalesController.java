package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantSalesVO;
import com.gzu.adminconsole.jingchen.service.MerchantPromoService;

/**
 * 商户销售报表接口（jingchen 模块）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/sales")
public class MerchantSalesController {

    private final MerchantPromoService service;

    public MerchantSalesController(MerchantPromoService service) {
        this.service = service;
    }

    @GetMapping
    public Result<MerchantSalesVO> sales() {
        return Result.ok(service.sales());
    }
}
