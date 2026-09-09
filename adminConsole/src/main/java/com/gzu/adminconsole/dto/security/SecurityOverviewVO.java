package com.gzu.adminconsole.dto.security;

import java.util.List;

import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.common.ToggleItem;

/**
 * 安全风控、设备审计与 RBAC 权限视图模型（对应页面 a10）。
 */
public record SecurityOverviewVO(List<KpiMetric> kpis,
                                 List<RoleNode> roleTree,
                                 List<DeviceRow> devices,
                                 List<PlanRow> plans,
                                 List<AppUserRow> appUsers,
                                 List<AuditLogRow> auditLogs,
                                 List<AdminUserRow> admins,
                                 List<ToggleItem> policies,
                                 MapCard map,
                                 Pagination page) {

    /** 后台管理员账号。 */
    public record AdminUserRow(Long id, String name, String role, String group, String phone, String status,
                               String lastLogin) {
    }

    /** C 端用户账号行。 */
    public record AppUserRow(Long id, String account, String regSource, String membership,
                             String registered, String lastActive, String status) {
    }

    /** 角色域。 */
    public record RoleNode(String name, String code, String icon, int members, List<PermissionGroup> groups) {
    }

    /** 权限组。 */
    public record PermissionGroup(String name, int members, List<PermissionItem> permissions) {
    }

    /** 细粒度权限项（三态）。 */
    public record PermissionItem(String name, String state) {
    }

    /** 异常设备监控行。 */
    public record DeviceRow(String region,
                            String ip,
                            String fingerprint,
                            String sessions,
                            int risk,
                            String verdict,
                            String tone,
                            boolean banned) {
    }

    /** 会员套餐与额度配置。 */
    public record PlanRow(String name, String price, String desc, String quota, String subscribers,
                          String usage) {
    }

    /** 管理员操作日志（追加写入 + 哈希链存证）。 */
    public record AuditLogRow(Long id,
                              String time,
                              String operator,
                              String role,
                              String group,
                              String action,
                              String detail,
                              String source,
                              String result,
                              String hash) {
    }

    /** 登录监控地图。 */
    public record MapCard(String hub, List<MapPoint> points) {
    }

    /** 地图监控点位。 */
    public record MapPoint(String name, double lon, double lat, int sessions, String level) {
    }

    /** 分页信息。 */
    public record Pagination(int page, int size, long total, int totalPages, String rangeText,
                             int retentionDays) {
    }
}
