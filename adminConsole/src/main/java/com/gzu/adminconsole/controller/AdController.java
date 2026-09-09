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
import com.gzu.adminconsole.dto.ads.AdOverviewVO;
import com.gzu.adminconsole.dto.ads.FrequencyUpdateRequest;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.model.AdSlot;
import com.gzu.adminconsole.service.AdService;

/**
 * 广告位排期与调度接口（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/ads")
@RequireRole({"SUPER_ADMIN", "OPERATIONS", "AUDITOR"})
public class AdController {

    private final AdService service;

    public AdController(AdService service) {
        this.service = service;
    }

    /** 广告排期大盘（可按排期日期范围过滤，yyyy-MM-dd）。 */
    @GetMapping("/overview")
    public Result<AdOverviewVO> overview(@RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Result.ok(service.overview(start, end));
    }

    /** 更新单用户频次限制（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/frequency")
    public Result<ActionResultVO> updateFrequency(@RequestBody FrequencyUpdateRequest request) {
        return Result.ok(service.updateFrequency(request));
    }

    /** 采纳 AI 调优建议（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/advice/adopt")
    public Result<ActionResultVO> adoptAdvice() {
        return Result.ok(service.adoptAdvice());
    }

    /* ------------------------------ 广告位 CRUD ------------------------------ */

    /** 新增广告位（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/slots")
    public Result<ActionResultVO> createSlot(@RequestBody AdSlot slot) {
        return Result.ok(service.createSlot(slot));
    }

    /** 修改广告位（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/slots")
    public Result<ActionResultVO> updateSlot(@RequestBody AdSlot slot) {
        return Result.ok(service.updateSlot(slot));
    }

    /** 删除广告位（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @DeleteMapping("/slots/{id}")
    public Result<ActionResultVO> deleteSlot(@PathVariable Long id) {
        return Result.ok(service.deleteSlot(id));
    }

    /** 上线 / 下线广告位：online = true 上线 / false 下线（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PatchMapping("/slots/{id}/online")
    public Result<ActionResultVO> toggleSlotOnline(@PathVariable Long id, @RequestParam boolean online) {
        return Result.ok(service.toggleSlotOnline(id, online));
    }
}
