package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.dto.security.PermissionUpdateRequest;
import com.gzu.adminconsole.dto.security.SecurityOverviewVO;
import com.gzu.adminconsole.model.AdminUser;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.model.DeviceRecord;
import com.gzu.adminconsole.model.MembershipPlan;
import com.gzu.adminconsole.model.PermGroup;
import com.gzu.adminconsole.model.Permission;
import com.gzu.adminconsole.model.RoleDomain;
import com.gzu.adminconsole.repository.MetricRepository;
import com.gzu.adminconsole.repository.SecurityRepository;

/**
 * 安全风控、设备审计与 RBAC 权限 ViewModel 层。
 */
@Service
public class SecurityService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 演示用固定来源 IP / 地域。 */
    private static final String OPERATOR_SOURCE = "203.208.60.12 · 上海";
    /** 演示用当前操作人。 */
    private static final String OPERATOR = "Danny";
    /** 演示用当前操作人角色 / 权限组。 */
    private static final String OPERATOR_ROLE = "超级管理员";
    private static final String OPERATOR_GROUP = "安全审计组";

    private final SecurityRepository repository;
    private final AppProperties properties;
    private final GeocodingService geocoding;
    private final MetricRepository metrics;

    public SecurityService(SecurityRepository repository, AppProperties properties, GeocodingService geocoding,
                           MetricRepository metrics) {
        this.repository = repository;
        this.properties = properties;
        this.geocoding = geocoding;
        this.metrics = metrics;
    }

    /** 安全风控大盘视图模型（首页）。 */
    public SecurityOverviewVO overview() {
        return overview(1, SecurityRepository.AUDIT_PAGE_SIZE, null, null);
    }

    /** 安全风控大盘视图模型（指定审计日志页码，页大小取默认值）。 */
    public SecurityOverviewVO overview(int page) {
        return overview(page, SecurityRepository.AUDIT_PAGE_SIZE, null, null);
    }

    /** 安全风控大盘视图模型（指定审计日志页码与时间范围，yyyy-MM-dd）。 */
    public SecurityOverviewVO overview(int page, String start, String end) {
        return overview(page, SecurityRepository.AUDIT_PAGE_SIZE, start, end);
    }

    /** 安全风控大盘视图模型（可指定审计日志页码、页大小与时间范围）。 */
    public SecurityOverviewVO overview(int page, int size, String start, String end) {
        List<DeviceRecord> devices = repository.findDevices();
        long banned = devices.stream().filter(DeviceRecord::banned).count();
        // 异常设备 = 判定为异常 / 可疑的设备（真实统计，新增设备后会自动变化）
        long abnormal = devices.stream()
                .filter(d -> "异常".equals(d.verdict()) || "可疑".equals(d.verdict()))
                .count();
        long adminCount = repository.countAdmins();

        // 越权拦截次数与日志趋势均取自真实落库数据，随所选日期范围变化
        long blocked = (long) metrics.sum(MetricRepository.MetricKey.SECURITY_BLOCK, start, end);
        List<Double> blockTrend = dailyTrend(MetricRepository.MetricKey.SECURITY_BLOCK, start, end, blocked);
        List<Double> logTrend = logTrend(start, end);

        List<KpiMetric> kpis = List.of(
                new KpiMetric("管理员账号总数", String.valueOf(adminCount), null, "fa-user-shield", "#1E3A8A",
                        "#2563EB", null, null, null, roleSummary(),
                        trend(adminCount)),
                new KpiMetric("异常设备", String.valueOf(abnormal), null, "fa-mobile-screen-button",
                        "#B91C1C", "#EF4444", null, null, null,
                        "已封禁 " + banned + " · 待复核 " + abnormal + " · 台账 " + devices.size() + " 台",
                        trend(abnormal)),
                new KpiMetric("越权访问拦截", String.valueOf(blocked), null, "fa-hand", "#B45309", "#F59E0B",
                        deltaOf(blockTrend), rising(blockTrend), "green",
                        "所选范围内累计拦截 · 全部已阻断", blockTrend),
                new KpiMetric("操作日志留存", String.valueOf(properties.getSecurity().getLogRetentionDays()),
                        " 天", "fa-link", "#0B1E4D", "#1E3A8A", null, null, null,
                        "区块链存证 · 累计 " + formatCount(repository.totalLogs(null, null)) + " 条",
                        logTrend));

        return new SecurityOverviewVO(kpis, roleTree(), deviceRows(devices), plans(), auditLogs(page, size, start, end),
                admins(), repository.findPolicies(), mapCard(), pagination(page, size, start, end));
    }

    /* ------------------------------ 设备 ------------------------------ */

    /** 封禁异常设备，并追加一条不可篡改的操作日志。 */
    public ActionResultVO banDevice(String fingerprint) {
        DeviceRecord device = repository.findDevice(fingerprint);
        if (device == null) {
            throw new BusinessException("未找到设备：" + fingerprint);
        }
        if (device.banned()) {
            throw new BusinessException("设备 " + fingerprint + " 已处于封禁状态");
        }
        repository.updateDevice(device.markBanned());
        writeLog("异常设备封禁", "封禁异常登录设备 " + fingerprint);
        return ActionResultVO.ok("设备 " + fingerprint + " 已封禁", fingerprint);
    }

    /** 新增设备。 */
    public ActionResultVO createDevice(DeviceRecord device) {
        requireText(device.fingerprint(), "设备指纹");
        if (repository.findDevice(device.fingerprint()) != null) {
            throw new BusinessException("设备指纹已存在：" + device.fingerprint());
        }
        repository.insertDevice(device);
        writeLog("设备台账维护", "新增设备台账 " + device.fingerprint());
        return ActionResultVO.ok("设备 " + device.fingerprint() + " 已新增", device.fingerprint());
    }

    /** 更新设备。 */
    public ActionResultVO updateDevice(DeviceRecord device) {
        requireText(device.fingerprint(), "设备指纹");
        if (repository.findDevice(device.fingerprint()) == null) {
            throw new BusinessException("未找到设备：" + device.fingerprint());
        }
        repository.updateDevice(device);
        writeLog("设备台账维护", "更新设备台账 " + device.fingerprint());
        return ActionResultVO.ok("设备 " + device.fingerprint() + " 已更新", device.fingerprint());
    }

    /** 删除设备。 */
    public ActionResultVO deleteDevice(String fingerprint) {
        repository.deleteDevice(fingerprint);
        writeLog("设备台账维护", "删除设备台账 " + fingerprint);
        return ActionResultVO.ok("设备 " + fingerprint + " 已删除", fingerprint);
    }

    /* ------------------------------ 套餐 ------------------------------ */

    /** 新增套餐。 */
    public ActionResultVO createPlan(MembershipPlan plan) {
        requireText(plan.name(), "套餐名称");
        if (repository.findPlan(plan.name()) != null) {
            throw new BusinessException("套餐已存在：" + plan.name());
        }
        repository.insertPlan(plan);
        writeLog("套餐与额度配置", "新增会员套餐 " + plan.name());
        return ActionResultVO.ok("套餐「" + plan.name() + "」已新增", plan.name());
    }

    /** 更新套餐。 */
    public ActionResultVO updatePlan(MembershipPlan plan) {
        requireText(plan.name(), "套餐名称");
        if (repository.findPlan(plan.name()) == null) {
            throw new BusinessException("未找到套餐：" + plan.name());
        }
        repository.updatePlan(plan);
        writeLog("套餐与额度配置", "更新会员套餐 " + plan.name() + "（额度 " + plan.quota() + "）");
        return ActionResultVO.ok("套餐「" + plan.name() + "」已更新", plan.name());
    }

    /** 删除套餐。 */
    public ActionResultVO deletePlan(String name) {
        repository.deletePlan(name);
        writeLog("套餐与额度配置", "删除会员套餐 " + name);
        return ActionResultVO.ok("套餐「" + name + "」已删除", name);
    }

    /* ------------------------------ 管理员 ------------------------------ */

    /** 新增管理员。 */
    public ActionResultVO createAdmin(AdminUser user) {
        requireText(user.name(), "管理员姓名");
        repository.insertAdmin(user);
        writeLog("管理员账号管理", "新增管理员 " + user.name() + "（" + user.role() + "）");
        return ActionResultVO.ok("管理员 " + user.name() + " 已新增", user.name());
    }

    /** 更新管理员。 */
    public ActionResultVO updateAdmin(AdminUser user) {
        if (user.id() == null || repository.findAdmin(user.id()) == null) {
            throw new BusinessException("未找到该管理员");
        }
        repository.updateAdmin(user);
        writeLog("管理员账号管理", "更新管理员 " + user.name() + "（状态 " + user.status() + "）");
        return ActionResultVO.ok("管理员 " + user.name() + " 已更新", user.name());
    }

    /** 删除管理员。 */
    public ActionResultVO deleteAdmin(Long id) {
        AdminUser user = repository.findAdmin(id);
        if (user == null) {
            throw new BusinessException("未找到该管理员");
        }
        repository.deleteAdmin(id);
        writeLog("管理员账号管理", "删除管理员 " + user.name());
        return ActionResultVO.ok("管理员 " + user.name() + " 已删除", user.name());
    }

    /* ---------------------------- 安全策略 ---------------------------- */

    /** 切换安全策略开关。 */
    public ActionResultVO updatePolicy(String name, boolean enabled) {
        repository.updatePolicy(name, enabled);
        writeLog("安全策略配置", (enabled ? "开启" : "关闭") + "安全策略「" + name + "」");
        return ActionResultVO.ok("安全策略「" + name + "」已" + (enabled ? "开启" : "关闭"), name);
    }

    /* ---------------------------- RBAC 授权 ---------------------------- */

    /** 更新 RBAC 三态授权。 */
    public ActionResultVO updatePermission(PermissionUpdateRequest request) {
        if (request == null || request.roleCode() == null || request.groupName() == null
                || request.permissionName() == null || request.state() == null) {
            throw new BusinessException("权限更新参数不完整");
        }
        String state = request.state().toUpperCase();
        if (!List.of(Permission.GRANTED, Permission.PARTIAL, Permission.NONE).contains(state)) {
            throw new BusinessException("非法的授权状态：" + request.state());
        }

        RoleDomain role = repository.findRole(request.roleCode());
        if (role == null) {
            throw new BusinessException("未找到角色：" + request.roleCode());
        }

        boolean matched = false;
        List<PermGroup> newGroups = new ArrayList<>();
        for (PermGroup group : role.groups()) {
            if (!group.name().equals(request.groupName())) {
                newGroups.add(group);
                continue;
            }
            List<Permission> newPermissions = new ArrayList<>();
            for (Permission permission : group.permissions()) {
                if (permission.name().equals(request.permissionName())) {
                    newPermissions.add(permission.withState(state));
                    matched = true;
                } else {
                    newPermissions.add(permission);
                }
            }
            newGroups.add(new PermGroup(group.name(), group.members(), newPermissions));
        }
        if (!matched) {
            throw new BusinessException("未找到权限项：" + request.permissionName());
        }

        List<RoleDomain> roles = new ArrayList<>(repository.findRoles());
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).code().equalsIgnoreCase(role.code())) {
                roles.set(i, new RoleDomain(role.name(), role.code(), role.icon(), role.members(), newGroups));
            }
        }
        repository.saveRoles(roles);
        writeLog("RBAC 权限配置", "调整「" + role.name() + " / " + request.groupName() + "」的「"
                + request.permissionName() + "」为 " + state);
        return ActionResultVO.ok("权限已更新并已写入审计日志", request.permissionName());
    }

    /* ------------------------------ 私有方法 ------------------------------ */

    private List<SecurityOverviewVO.RoleNode> roleTree() {
        return repository.findRoles().stream()
                .map(role -> new SecurityOverviewVO.RoleNode(role.name(), role.code(), role.icon(),
                        role.members(), role.groups().stream()
                        .map(g -> new SecurityOverviewVO.PermissionGroup(g.name(), g.members(),
                                g.permissions().stream()
                                        .map(p -> new SecurityOverviewVO.PermissionItem(p.name(), p.state()))
                                        .toList()))
                        .toList()))
                .toList();
    }

    private List<SecurityOverviewVO.DeviceRow> deviceRows(List<DeviceRecord> devices) {
        return devices.stream()
                .map(d -> new SecurityOverviewVO.DeviceRow(d.region(), d.ip(), d.fingerprint(), d.sessions(),
                        d.risk(), d.verdict(), toneOf(d.verdict()), d.banned()))
                .toList();
    }

    private String toneOf(String verdict) {
        return switch (verdict) {
            case "异常", "已封禁" -> "red";
            case "可疑" -> "amber";
            default -> "green";
        };
    }

    private List<SecurityOverviewVO.PlanRow> plans() {
        return repository.findPlans().stream()
                .map(p -> new SecurityOverviewVO.PlanRow(p.name(), p.price(), p.desc(), p.quota(),
                        p.subscribers(), p.usage()))
                .toList();
    }

    private List<SecurityOverviewVO.AuditLogRow> auditLogs(int page, int size, String start, String end) {
        return repository.findAuditLogs(page, size, start, end).stream()
                .map(l -> new SecurityOverviewVO.AuditLogRow(l.id(), l.time(), l.operator(), l.role(), l.group(),
                        l.action(), l.detail(), l.source(), l.result(), l.hash()))
                .toList();
    }

    private List<SecurityOverviewVO.AdminUserRow> admins() {
        return repository.findAdmins().stream()
                .map(u -> new SecurityOverviewVO.AdminUserRow(u.id(), u.name(), u.role(), u.group(), u.phone(),
                        u.status(), u.lastLogin()))
                .toList();
    }

    /**
     * 登录与会话热力地图：实时由设备台账派生，保证新增 / 改判 / 封禁设备后地图同步变化。
     * 同一城市的设备合并为一个点位（会话数累加，风险等级取最严重者）。
     */
    private SecurityOverviewVO.MapCard mapCard() {
        // city -> { sessions, severity }；坐标由地理编码服务动态解析（腾讯 LBS / Open-Meteo + 数据库缓存），不再硬编码
        Map<String, int[]> summary = new LinkedHashMap<>();
        Map<String, double[]> coords = new LinkedHashMap<>();
        repository.findDevices().forEach((DeviceRecord d) -> {
            String city = cityOf(d.region());
            double[] coord = geocoding.resolve(city);
            if (coord == null) {
                return; // 城市无法解析（外网异常等）时跳过该设备，不影响其余点位
            }
            coords.putIfAbsent(city, coord);
            int severity = d.banned() || "异常".equals(d.verdict()) || "已封禁".equals(d.verdict()) ? 2
                    : "可疑".equals(d.verdict()) ? 1 : 0;
            int[] acc = summary.computeIfAbsent(city, key -> new int[] {0, 0});
            acc[0] += parseSessions(d.sessions());
            acc[1] = Math.max(acc[1], severity);
        });

        List<SecurityOverviewVO.MapPoint> rows = summary.entrySet().stream()
                .map(e -> {
                    double[] coord = coords.get(e.getKey());
                    return new SecurityOverviewVO.MapPoint(e.getKey(), coord[0], coord[1], e.getValue()[0],
                            e.getValue()[1] == 2 ? "bad" : e.getValue()[1] == 1 ? "warn" : "ok");
                })
                .toList();
        String hottest = rows.stream()
                .max(Comparator.comparingInt(SecurityOverviewVO.MapPoint::sessions))
                .map(SecurityOverviewVO.MapPoint::name)
                .orElse("上海");
        return new SecurityOverviewVO.MapCard(hottest, rows);
    }

    /** 会话数文案 "1,286" → 数值。 */
    private int parseSessions(String sessions) {
        if (sessions == null) {
            return 0;
        }
        String digits = sessions.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    /** 地域文案 "上海 · 电信" → 城市名。 */
    private String cityOf(String region) {
        if (region == null) {
            return "";
        }
        return region.split("·")[0].trim();
    }

    private SecurityOverviewVO.Pagination pagination(int page, int size, String start, String end) {
        long total = repository.totalLogs(start, end);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / Math.max(1, size)));
        int safePage = Math.min(Math.max(1, page), totalPages);
        int from = total == 0 ? 0 : (safePage - 1) * size + 1;
        int to = (int) Math.min(total, (long) safePage * size);
        return new SecurityOverviewVO.Pagination(safePage, size, total, totalPages,
                "显示 " + from + "–" + to + " 条，共 " + formatCount(total) + " 条",
                properties.getSecurity().getLogRetentionDays());
    }

    /** 取某指标按天聚合的最后 12 个点作为趋势迷你图；无真实数据时回退为收敛曲线。 */
    private List<Double> dailyTrend(String metricKey, String start, String end, double fallback) {
        List<Double> values = new ArrayList<>(metrics.sumByDate(metricKey, start, end).values());
        if (values.isEmpty()) {
            return trend(fallback);
        }
        return values.size() > 12 ? values.subList(values.size() - 12, values.size()) : values;
    }

    /** 审计日志按天计数（真实数据），作为"操作日志留存"的趋势。 */
    private List<Double> logTrend(String start, String end) {
        List<Double> values = new ArrayList<>();
        repository.countLogsByDate(start, end).values().forEach(v -> values.add(v.doubleValue()));
        if (values.isEmpty()) {
            return trend(repository.totalLogs(null, null));
        }
        return values.size() > 12 ? values.subList(values.size() - 12, values.size()) : values;
    }

    /** 由趋势序列首尾计算环比百分比文案。 */
    private static String deltaOf(List<Double> series) {
        if (series.size() < 2) {
            return "0.0%";
        }
        double first = series.get(0);
        double last = series.get(series.size() - 1);
        if (first <= 0) {
            return "0.0%";
        }
        return String.format("%.1f%%", (last - first) * 100.0 / first);
    }

    /** 趋势方向：末尾值不低于起点即视为上升。 */
    private static boolean rising(List<Double> series) {
        return series.size() < 2 || series.get(series.size() - 1) >= series.get(0);
    }

    /** 由当前值生成收敛曲线，用于无时间维度数据（如账号 / 设备台账）的兜底趋势。 */
    private static List<Double> trend(double current) {
        double[] ratios = {0.58, 0.64, 0.70, 0.75, 0.80, 0.85, 0.89, 0.92, 0.95, 0.97, 0.99, 1.0};
        List<Double> out = new ArrayList<>();
        for (double ratio : ratios) {
            out.add(Math.round(current * ratio * 100.0) / 100.0);
        }
        return out;
    }

    /** 角色人数分布文案。 */
    private String roleSummary() {
        long total = repository.countAdmins();
        long superAdmin = repository.findAdmins().stream()
                .filter(u -> "超级管理员".equals(u.role())).count();
        long auditor = repository.findAdmins().stream()
                .filter(u -> "只读审计员".equals(u.role())).count();
        return "超管 " + superAdmin + " · 运营 " + (total - superAdmin - auditor) + " · 审计 " + auditor;
    }

    private void writeLog(String action, String detail) {
        AuditLogEntry entry = AuditLogEntry.of(now(), OPERATOR, OPERATOR_ROLE, OPERATOR_GROUP, action, detail,
                OPERATOR_SOURCE, "成功");
        repository.pushAuditLog(entry);
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
    }

    private static String formatCount(long value) {
        return value >= 10_000 ? String.format("%.1f 万", value / 10_000.0) : String.valueOf(value);
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private static String hash() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
