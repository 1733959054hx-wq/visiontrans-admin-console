package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.dto.moderation.ModerationOverviewVO;
import com.gzu.adminconsole.model.GlossaryTask;
import com.gzu.adminconsole.model.MaterialAsset;
import com.gzu.adminconsole.service.ModerationService;

/**
 * 术语库审核与 UGC 风控接口（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/moderation")
@RequireRole({"SUPER_ADMIN", "OPERATIONS", "AUDITOR"})
public class ModerationController {

    private final ModerationService service;

    public ModerationController(ModerationService service) {
        this.service = service;
    }

    /** 审核与风控中台大盘（可按任务截止时间范围过滤，yyyy-MM-dd）。 */
    @GetMapping("/overview")
    public Result<ModerationOverviewVO> overview(@RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Result.ok(service.overview(start, end));
    }

    /** UGC 违规处置：ban / pass / review（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/ugc/decision")
    public Result<ActionResultVO> decide(@RequestParam(defaultValue = "review") String action) {
        return Result.ok(service.decideUgc(action));
    }

    /** 按平台建议执行退款（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/refund/process")
    public Result<ActionResultVO> processRefund() {
        return Result.ok(service.processRefund());
    }

    /* ------------------------------ 术语包任务 CRUD ------------------------------ */

    /** 新增术语包任务（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/tasks")
    public Result<ActionResultVO> createTask(@RequestBody GlossaryTask task) {
        return Result.ok(service.createTask(task));
    }

    /** 修改术语包任务（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/tasks")
    public Result<ActionResultVO> updateTask(@RequestBody GlossaryTask task) {
        return Result.ok(service.updateTask(task));
    }

    /** 移动术语包任务（direction = prev / next；运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PatchMapping("/tasks/{id}/move")
    public Result<ActionResultVO> moveTask(@PathVariable Long id,
                                           @RequestParam(defaultValue = "next") String direction) {
        return Result.ok(service.moveTask(id, direction));
    }

    /** 删除术语包任务（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @DeleteMapping("/tasks/{id}")
    public Result<ActionResultVO> deleteTask(@PathVariable Long id) {
        return Result.ok(service.deleteTask(id));
    }

    /* ------------------------------ 素材机审 CRUD ------------------------------ */

    /** 新增素材（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/assets")
    public Result<ActionResultVO> createAsset(@RequestBody MaterialAsset asset) {
        return Result.ok(service.createAsset(asset));
    }

    /** 修改素材（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/assets")
    public Result<ActionResultVO> updateAsset(@RequestBody MaterialAsset asset) {
        return Result.ok(service.updateAsset(asset));
    }

    /** 删除素材（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @DeleteMapping("/assets/{id}")
    public Result<ActionResultVO> deleteAsset(@PathVariable Long id) {
        return Result.ok(service.deleteAsset(id));
    }
}
