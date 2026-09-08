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
import com.gzu.adminconsole.jingchen.dto.MerchantAbRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantMaterialRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantAbConfigEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantMaterialEntity;
import com.gzu.adminconsole.jingchen.service.MerchantMaterialService;

/**
 * 商户素材管理接口（jingchen 模块,含 A/B 测试配置）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/materials")
public class MerchantMaterialController {

    private final MerchantMaterialService service;

    public MerchantMaterialController(MerchantMaterialService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<MerchantMaterialEntity>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<MerchantMaterialEntity> create(@RequestBody MerchantMaterialRequest req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Result<MerchantMaterialEntity> update(@PathVariable Long id, @RequestBody MerchantMaterialRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(true);
    }

    /** A/B 测试配置读取。 */
    @GetMapping("/ab")
    public Result<MerchantAbConfigEntity> ab() {
        return Result.ok(service.ab());
    }

    /** A/B 测试配置保存。 */
    @PutMapping("/ab")
    public Result<MerchantAbConfigEntity> saveAb(@RequestBody MerchantAbRequest req) {
        return Result.ok(service.saveAb(req));
    }
}
