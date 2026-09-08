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
import com.gzu.adminconsole.jingchen.dto.MerchantVideoRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;
import com.gzu.adminconsole.jingchen.service.MerchantVideoService;

/**
 * 商户视频接入接口（jingchen 模块）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/videos")
public class MerchantVideoController {

    private final MerchantVideoService service;

    public MerchantVideoController(MerchantVideoService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<MerchantVideoEntity>> list() {
        return Result.ok(service.list());
    }

    /** 上传(演示:建档案,状态默认转码中)。 */
    @PostMapping
    public Result<MerchantVideoEntity> create(@RequestBody MerchantVideoRequest req) {
        return Result.ok(service.create(req));
    }

    /** 元数据编辑(标题 / 语种 / 状态 / 授权区域等)。 */
    @PutMapping("/{id}")
    public Result<MerchantVideoEntity> update(@PathVariable Long id, @RequestBody MerchantVideoRequest req) {
        return Result.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(true);
    }
}
