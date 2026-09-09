package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.dto.cluster.BreakerUpdateRequest;
import com.gzu.adminconsole.dto.cluster.BackupUpdateRequest;
import com.gzu.adminconsole.dto.cluster.OpsPanelVO;
import com.gzu.adminconsole.dto.cluster.SysLogVO;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.service.OpsService;

/**
 * 监控运维接口：系统日志、熔断降级、备份策略与告警阈值（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/cluster")
@RequireRole({"SUPER_ADMIN", "OPERATIONS", "AUDITOR"})
public class OpsController {

    private final OpsService service;

    public OpsController(OpsService service) {
        this.service = service;
    }

    /** 监控运维面板（熔断降级 / 备份策略 / 告警阈值）。 */
    @GetMapping("/ops")
    public Result<OpsPanelVO> opsPanel() {
        return Result.ok(service.opsPanel());
    }

    /** 系统日志（level / category 可空表示不过滤）。 */
    @GetMapping("/syslogs")
    public Result<SysLogVO> sysLogs(@RequestParam(required = false) String level,
                                    @RequestParam(required = false) String category) {
        return Result.ok(service.sysLogs(level, category));
    }

    /** 更新熔断降级策略（状态 + 启用，运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/ops/breakers")
    public Result<ActionResultVO> updateBreaker(@RequestBody BreakerUpdateRequest request) {
        return Result.ok(service.updateBreaker(request));
    }

    /** 启停备份策略（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/ops/backups")
    public Result<ActionResultVO> updateBackup(@RequestBody BackupUpdateRequest request) {
        return Result.ok(service.updateBackup(request));
    }

    /** 更新告警阈值（cpu / mem / gpu 利用率 %，运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/ops/thresholds")
    public Result<ActionResultVO> updateThresholds(@RequestParam int cpu, @RequestParam int mem,
                                                   @RequestParam int gpu) {
        return Result.ok(service.updateThresholds(cpu, mem, gpu));
    }
}
