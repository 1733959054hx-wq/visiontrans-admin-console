package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantFundAmountRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantFundsVO;
import com.gzu.adminconsole.jingchen.entity.MerchantFundFlowEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantWithdrawEntity;
import com.gzu.adminconsole.jingchen.service.MerchantFundsService;

/**
 * 商户资金接口（jingchen 模块,账户管理-资金账户 / 资金流水 / 提现管理）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/funds")
public class MerchantFundsController {

    private final MerchantFundsService service;

    public MerchantFundsController(MerchantFundsService service) {
        this.service = service;
    }

    /** 资金总览:余额 / 收支合计 / 流水 / 提现记录。 */
    @GetMapping
    public Result<MerchantFundsVO> overview() {
        return Result.ok(service.overview());
    }

    /** 在线充值。 */
    @PostMapping("/recharge")
    public Result<MerchantFundFlowEntity> recharge(@RequestBody MerchantFundAmountRequest req) {
        return Result.ok(service.recharge(req));
    }

    /** 提现申请(冻结余额,状态审核中)。 */
    @PostMapping("/withdraw")
    public Result<MerchantWithdrawEntity> withdraw(@RequestBody MerchantFundAmountRequest req) {
        return Result.ok(service.withdraw(req));
    }
}
