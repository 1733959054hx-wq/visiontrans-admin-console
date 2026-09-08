package com.gzu.adminconsole.jingchen.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantChannelRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantChannelEntity;
import com.gzu.adminconsole.jingchen.service.MerchantPromoService;

/**
 * 商户推广渠道接口（jingchen 模块）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/promotions")
public class MerchantPromoController {

    private final MerchantPromoService service;

    public MerchantPromoController(MerchantPromoService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<MerchantChannelEntity>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<MerchantChannelEntity> create(@RequestBody MerchantChannelRequest req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Result<MerchantChannelEntity> update(@PathVariable Long id, @RequestBody MerchantChannelRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(true);
    }
}
