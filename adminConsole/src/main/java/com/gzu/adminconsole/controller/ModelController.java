package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.dto.model.ModelOverviewVO;
import com.gzu.adminconsole.model.ModelRelease;
import com.gzu.adminconsole.service.ModelService;

/**
 * AI 模型生命周期与热更接口（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/models")
public class ModelController {

    private final ModelService service;

    public ModelController(ModelService service) {
        this.service = service;
    }

    /** 模型热更中心大盘。 */
    @GetMapping("/overview")
    public Result<ModelOverviewVO> overview(@RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Result.ok(service.overview(start, end));
    }

    /** 调整全局灰度比例。 */
    @PutMapping("/grayscale")
    public Result<ActionResultVO> updateGrayscale(@RequestParam int ratio) {
        return Result.ok(service.updateGrayscale(ratio));
    }

    /** 指定模型秒级热更到全量。 */
    @PostMapping("/hot-update")
    public Result<ActionResultVO> hotUpdate(@RequestParam String name) {
        return Result.ok(service.hotUpdate(name));
    }

    /** 指定模型一键回滚。 */
    @PostMapping("/rollback")
    public Result<ActionResultVO> rollback(@RequestParam String name) {
        return Result.ok(service.rollback(name));
    }

    /* ------------------------------ 模型 CRUD ------------------------------ */

    /** 登记新模型版本。 */
    @PostMapping
    public Result<ActionResultVO> create(@RequestBody ModelRelease model) {
        return Result.ok(service.createModel(model));
    }

    /** 修改模型配置。 */
    @PutMapping
    public Result<ActionResultVO> update(@RequestBody ModelRelease model) {
        return Result.ok(service.updateModel(model));
    }

    /** 删除模型版本。 */
    @DeleteMapping
    public Result<ActionResultVO> delete(@RequestParam String name) {
        return Result.ok(service.deleteModel(name));
    }

    /** 切换灰度 / 热更策略开关。 */
    @PutMapping("/strategies")
    public Result<ActionResultVO> updateStrategy(@RequestParam String name, @RequestParam boolean enabled) {
        return Result.ok(service.updateStrategy(name, enabled));
    }
}
