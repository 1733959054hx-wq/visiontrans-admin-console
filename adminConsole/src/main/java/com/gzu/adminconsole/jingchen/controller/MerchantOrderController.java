package com.gzu.adminconsole.jingchen.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.entity.MerchantOrderEntity;
import com.gzu.adminconsole.jingchen.service.MerchantOrderService;

/**
 * 商户订单与结算接口（jingchen 模块）。
 *
 * <p>类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问,与后台管理接口双向隔离。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/orders")
public class MerchantOrderController {

    private final MerchantOrderService service;

    public MerchantOrderController(MerchantOrderService service) {
        this.service = service;
    }

    /** 订单列表(最新在前)。 */
    @GetMapping
    public Result<List<MerchantOrderEntity>> list() {
        return Result.ok(service.list());
    }

    /** 结算一笔待结算订单。 */
    @PutMapping("/{id}/settle")
    public Result<MerchantOrderEntity> settle(@PathVariable Long id) {
        return Result.ok(service.settle(id));
    }
}
