package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.gzu.adminconsole.dto.security.PermissionUpdateRequest;
import com.gzu.adminconsole.dto.security.SecurityOverviewVO;
import com.gzu.adminconsole.dto.security.SysConfigVO;
import com.gzu.adminconsole.model.AdminUser;
import com.gzu.adminconsole.model.AppUser;
import com.gzu.adminconsole.model.DeviceRecord;
import com.gzu.adminconsole.model.MembershipPlan;
import com.gzu.adminconsole.service.SecurityService;

/**
 * 安全风控、设备审计与 RBAC 权限接口（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/security")
@RequireRole({"SUPER_ADMIN", "OPERATIONS", "AUDITOR"})
public class SecurityController {

    private final SecurityService service;

    public SecurityController(SecurityService service) {
        this.service = service;
    }

    /** 安全风控大盘（可指定审计日志页码与时间范围 yyyy-MM-dd）。 */
    @GetMapping("/overview")
    public Result<SecurityOverviewVO> overview(@RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String start, @RequestParam(required = false) String end) {
        return Result.ok(service.overview(page, start, end));
    }

    /** 封禁异常设备（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/devices/ban")
    public Result<ActionResultVO> banDevice(@RequestParam String fingerprint) {
        return Result.ok(service.banDevice(fingerprint));
    }

    /** 保存 RBAC 三态授权（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @PutMapping("/permissions")
    public Result<ActionResultVO> updatePermission(@RequestBody PermissionUpdateRequest request) {
        return Result.ok(service.updatePermission(request));
    }

    /** 切换安全策略开关（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @PutMapping("/policies")
    public Result<ActionResultVO> updatePolicy(@RequestParam String name, @RequestParam boolean enabled) {
        return Result.ok(service.updatePolicy(name, enabled));
    }

    /* ------------------------------ 设备 CRUD ------------------------------ */

    /** 新增设备台账（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/devices")
    public Result<ActionResultVO> createDevice(@RequestBody DeviceRecord device) {
        return Result.ok(service.createDevice(device));
    }

    /** 修改设备台账（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/devices")
    public Result<ActionResultVO> updateDevice(@RequestBody DeviceRecord device) {
        return Result.ok(service.updateDevice(device));
    }

    /** 删除设备台账（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @DeleteMapping("/devices/{fingerprint}")
    public Result<ActionResultVO> deleteDevice(@PathVariable String fingerprint) {
        return Result.ok(service.deleteDevice(fingerprint));
    }

    /* ------------------------------ 套餐 CRUD ------------------------------ */

    /** 新增会员套餐（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/plans")
    public Result<ActionResultVO> createPlan(@RequestBody MembershipPlan plan) {
        return Result.ok(service.createPlan(plan));
    }

    /** 修改会员套餐（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/plans")
    public Result<ActionResultVO> updatePlan(@RequestBody MembershipPlan plan) {
        return Result.ok(service.updatePlan(plan));
    }

    /** 删除会员套餐（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @DeleteMapping("/plans/{name}")
    public Result<ActionResultVO> deletePlan(@PathVariable String name) {
        return Result.ok(service.deletePlan(name));
    }

    /* ------------------------------ C 端用户 ------------------------------ */

    /** 修改 C 端用户（会员状态 / 启停用，运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PutMapping("/app-users")
    public Result<ActionResultVO> updateAppUser(@RequestBody AppUser user) {
        return Result.ok(service.updateAppUser(user));
    }

    /* ------------------------------ 系统配置 ------------------------------ */

    /** 系统配置：运行参数 + C 端功能开关。 */
    @GetMapping("/sysconfig")
    public Result<SysConfigVO> sysConfig() {
        return Result.ok(service.sysConfig());
    }

    /** 修改运行参数（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @PutMapping("/sysparams")
    public Result<ActionResultVO> updateSysParam(@RequestParam String name, @RequestParam String value) {
        return Result.ok(service.updateSysParam(name, value));
    }

    /** 切换 C 端功能开关（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @PutMapping("/sysfeatures")
    public Result<ActionResultVO> updateFeature(@RequestParam String name, @RequestParam boolean enabled) {
        return Result.ok(service.updateFeature(name, enabled));
    }

    /* ------------------------------ 管理员 CRUD ------------------------------ */

    /** 新增管理员（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @PostMapping("/admins")
    public Result<ActionResultVO> createAdmin(@RequestBody AdminUser user) {
        return Result.ok(service.createAdmin(user));
    }

    /** 修改管理员（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @PutMapping("/admins")
    public Result<ActionResultVO> updateAdmin(@RequestBody AdminUser user) {
        return Result.ok(service.updateAdmin(user));
    }

    /** 删除管理员（仅超级管理员）。 */
    @RequireRole("SUPER_ADMIN")
    @DeleteMapping("/admins/{id}")
    public Result<ActionResultVO> deleteAdmin(@PathVariable Long id) {
        return Result.ok(service.deleteAdmin(id));
    }
}
