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
import com.gzu.adminconsole.jingchen.dto.MerchantGoodsRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantGoodsEntity;
import com.gzu.adminconsole.jingchen.service.MerchantGoodsService;

/**
 * 商户商品管理接口（jingchen 模块,内容分销-商品管理 / 定价策略）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/goods")
public class MerchantGoodsController {

    private final MerchantGoodsService service;

    public MerchantGoodsController(MerchantGoodsService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<MerchantGoodsEntity>> list() {
        return Result.ok(service.list());
    }

    /** 商品上架(新建)。 */
    @PostMapping
    public Result<MerchantGoodsEntity> create(@RequestBody MerchantGoodsRequest req) {
        return Result.ok(service.create(req));
    }

    /** 商品编辑(含折扣定价)。 */
    @PutMapping("/{id}")
    public Result<MerchantGoodsEntity> update(@PathVariable Long id, @RequestBody MerchantGoodsRequest req) {
        return Result.ok(service.update(id, req));
    }

    /** 上架。 */
    @PutMapping("/{id}/shelf")
    public Result<MerchantGoodsEntity> shelf(@PathVariable Long id) {
        return Result.ok(service.toggleShelf(id, true));
    }

    /** 下架。 */
    @PutMapping("/{id}/unshelf")
    public Result<MerchantGoodsEntity> unshelf(@PathVariable Long id) {
        return Result.ok(service.toggleShelf(id, false));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(true);
    }
}
