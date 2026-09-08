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
import com.gzu.adminconsole.jingchen.dto.MerchantPlanRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantPlanEntity;
import com.gzu.adminconsole.jingchen.service.MerchantPlanService;

/**
 * 商户投放计划接口（jingchen 模块）。
 *
 * <p>类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问,后台管理员调用返回 403,
 * 与后台管理接口保持双向隔离(与 {@code MerchantController} 同规约)。
 *
 * <p>身份一律由 {@link AuthInterceptor} 解析并写入 {@link AdminContext},
 * 本控制器不信任任何前端传入的账号参数。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/plans")
public class MerchantPlanController {

    private final MerchantPlanService service;

    public MerchantPlanController(MerchantPlanService service) {
        this.service = service;
    }

    /** 投放计划列表。 */
    @GetMapping
    public Result<List<MerchantPlanEntity>> list() {
        return Result.ok(service.list());
    }

    /** 计划详情。 */
    @GetMapping("/{id}")
    public Result<MerchantPlanEntity> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    /** 新建投放计划。 */
    @PostMapping
    public Result<MerchantPlanEntity> create(@RequestBody MerchantPlanRequest req) {
        return Result.ok(service.create(req));
    }

    /** 编辑投放计划(仅名称 / 形式 / 场景 / 预算 / 负责人 / CTR)。 */
    @PutMapping("/{id}")
    public Result<MerchantPlanEntity> update(@PathVariable Long id, @RequestBody MerchantPlanRequest req) {
        return Result.ok(service.update(id, req));
    }

    /** 暂停投放。 */
    @PutMapping("/{id}/pause")
    public Result<MerchantPlanEntity> pause(@PathVariable Long id) {
        return Result.ok(service.toggle(id, true));
    }

    /** 恢复投放。 */
    @PutMapping("/{id}/resume")
    public Result<MerchantPlanEntity> resume(@PathVariable Long id) {
        return Result.ok(service.toggle(id, false));
    }

    /** 删除投放计划。 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(true);
    }
}
