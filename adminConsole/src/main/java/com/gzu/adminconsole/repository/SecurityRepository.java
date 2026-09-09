package com.gzu.adminconsole.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.PasswordHasher;
import com.gzu.adminconsole.dto.common.ToggleItem;
import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.entity.AppSessionEntity;
import com.gzu.adminconsole.entity.AppSetting;
import com.gzu.adminconsole.entity.AppUserEntity;
import com.gzu.adminconsole.entity.AuditLogEntity;
import com.gzu.adminconsole.entity.DeviceEntity;
import com.gzu.adminconsole.entity.MembershipPlanEntity;
import com.gzu.adminconsole.entity.MonitorPointEntity;
import com.gzu.adminconsole.entity.PermGroupEntity;
import com.gzu.adminconsole.entity.PermissionEntity;
import com.gzu.adminconsole.entity.RoleEntity;
import com.gzu.adminconsole.entity.StrategyToggleEntity;
import com.gzu.adminconsole.model.AdminUser;
import com.gzu.adminconsole.model.AppUser;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.model.DeviceRecord;
import com.gzu.adminconsole.model.MembershipPlan;
import com.gzu.adminconsole.model.MonitorPoint;
import com.gzu.adminconsole.model.PermGroup;
import com.gzu.adminconsole.model.Permission;
import com.gzu.adminconsole.model.RoleDomain;

/**
 * 安全风控、设备审计与 RBAC 权限数据访问层（JPA 实现）。
 */
@Repository
@Transactional(readOnly = true)
public class SecurityRepository {

    /** 开关分组：安全策略。 */
    public static final String GROUP_SECURITY = "security";

    /** 开关分组：C 端功能开关。 */
    public static final String GROUP_FEATURES = "features";

    /** 系统参数键：C 端接口限流阈值（次/分钟）。 */
    public static final String KEY_RATE_LIMIT = "sys.rate_limit";
    /** 系统参数默认值：接口限流阈值。 */
    public static final String DEFAULT_RATE_LIMIT = "600";
    /** 系统参数键：会话超时时间（小时）。 */
    public static final String KEY_SESSION_TTL = "sys.session_ttl";
    /** 系统参数默认值：会话超时时间。 */
    public static final String DEFAULT_SESSION_TTL = "24";

    /** 审计日志默认页大小。 */
    public static final int AUDIT_PAGE_SIZE = 8;

    /** 新增管理员默认口令（首次登录后应修改）。 */
    public static final String DEFAULT_PASSWORD = "admin123";

    @PersistenceContext
    private EntityManager em;

    /** 角色域（含权限组与细粒度权限）。 */
    public List<RoleDomain> findRoles() {
        return em.createQuery("select r from RoleEntity r order by r.sortOrder", RoleEntity.class)
                .getResultList().stream().map(this::toRoleModel).toList();
    }

    /** 按角色编码查找。 */
    public RoleDomain findRole(String code) {
        RoleEntity entity = em.find(RoleEntity.class, code);
        return entity == null ? null : toRoleModel(entity);
    }

    /**
     * 定点更新单个权限项的状态。
     *
     * <p>不做全表删除重建，避免并发保存时互相覆盖、以及中途失败导致权限数据丢失。</p>
     */
    @Transactional
    public void updatePermissionState(String roleCode, String groupName, String permissionName, String state) {
        List<RoleEntity> roles = em.createQuery("select r from RoleEntity r where r.code = :c", RoleEntity.class)
                .setParameter("c", roleCode)
                .getResultList();
        for (RoleEntity role : roles) {
            for (PermGroupEntity group : role.getGroups()) {
                if (!group.getName().equals(groupName)) {
                    continue;
                }
                for (PermissionEntity permission : group.getPermissions()) {
                    if (permission.getName().equals(permissionName)) {
                        permission.setState(state);
                        em.merge(permission);
                    }
                }
            }
        }
    }

    /** 初次灌入角色域（仅用于数据初始化，运行期改权限请用 updatePermissionState）。 */
    @Transactional
    public void insertRoles(List<RoleDomain> newRoles) {
        em.createQuery("delete from PermissionEntity p").executeUpdate();
        em.createQuery("delete from PermGroupEntity g").executeUpdate();
        em.createQuery("delete from RoleEntity r").executeUpdate();
        em.flush();
        int roleOrder = 0;
        for (RoleDomain role : newRoles) {
            RoleEntity roleEntity = new RoleEntity(role.code(), role.name(), role.icon(), role.members(),
                    roleOrder++);
            int groupOrder = 0;
            List<PermGroupEntity> groupEntities = new ArrayList<>();
            for (PermGroup group : role.groups()) {
                PermGroupEntity groupEntity = new PermGroupEntity(group.name(), group.members(), groupOrder++);
                int permOrder = 0;
                List<PermissionEntity> permEntities = new ArrayList<>();
                for (Permission permission : group.permissions()) {
                    permEntities.add(new PermissionEntity(permission.name(), permission.state(), permOrder++));
                }
                groupEntity.setPermissions(permEntities);
                groupEntities.add(groupEntity);
            }
            roleEntity.setGroups(groupEntities);
            em.persist(roleEntity);
        }
    }

    /** 全部设备。 */
    public List<DeviceRecord> findDevices() {
        return em.createQuery("select d from DeviceEntity d order by d.sortOrder", DeviceEntity.class)
                .getResultList().stream()
                .map(d -> new DeviceRecord(d.getRegion(), d.getIp(), d.getFingerprint(), d.getSessions(),
                        d.getRisk(), d.getVerdict(), d.isBanned(), d.getAccount()))
                .toList();
    }

    /** 按设备指纹查找。 */
    public DeviceRecord findDevice(String fingerprint) {
        DeviceEntity entity = em.find(DeviceEntity.class, fingerprint);
        return entity == null ? null
                : new DeviceRecord(entity.getRegion(), entity.getIp(), entity.getFingerprint(),
                        entity.getSessions(), entity.getRisk(), entity.getVerdict(), entity.isBanned(),
                        entity.getAccount());
    }

    /** 新增设备。 */
    @Transactional
    public void insertDevice(DeviceRecord device) {
        Integer max = em.createQuery("select max(d.sortOrder) from DeviceEntity d", Integer.class).getSingleResult();
        em.persist(new DeviceEntity(device.fingerprint(), device.region(), device.ip(), device.sessions(),
                device.risk(), device.verdict(), device.banned(), device.account(), max == null ? 0 : max + 1));
    }

    /** 更新设备记录（整行覆盖）。 */
    @Transactional
    public void updateDevice(DeviceRecord device) {
        DeviceEntity entity = em.find(DeviceEntity.class, device.fingerprint());
        if (entity == null) {
            return;
        }
        entity.setRegion(device.region());
        entity.setIp(device.ip());
        entity.setSessions(device.sessions());
        entity.setRisk(device.risk());
        entity.setVerdict(device.verdict());
        entity.setBanned(device.banned());
        entity.setAccount(device.account());
        em.merge(entity);
    }

    /** 删除设备。 */
    @Transactional
    public void deleteDevice(String fingerprint) {
        DeviceEntity entity = em.find(DeviceEntity.class, fingerprint);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /* ------------------------ C 端登录会话（踢下线） ------------------------ */

    /**
     * 老库升级：为既有设备回填关联账号，并同步派生到 C 端会话表。
     *
     * <p>「移除指定设备登录态」需要设备能定位到账号与会话，老数据缺账号时先补齐。
     */
    @Transactional
    public void backfillDeviceAccounts(Map<String, String> accounts) {
        accounts.forEach((fingerprint, account) -> {
            DeviceEntity entity = em.find(DeviceEntity.class, fingerprint);
            if (entity != null && (entity.getAccount() == null || entity.getAccount().isBlank())) {
                entity.setAccount(account);
                em.merge(entity);
            }
        });
        // 会话表的账号由设备台账派生：已生成的空值会话一并修复
        em.createNativeQuery("update app_session s join device_record d on d.fingerprint = s.fingerprint"
                + " set s.account = d.account where s.account is null and d.account is not null")
                .executeUpdate();
    }

    /** 指定设备上当前有效的登录会话数。 */
    public long countActiveSessions(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return 0L;
        }
        Long count = em.createQuery(
                        "select count(s) from AppSessionEntity s where s.fingerprint = :fp and s.expireAt > :now",
                        Long.class)
                .setParameter("fp", fingerprint)
                .setParameter("now", LocalDateTime.now())
                .getSingleResult();
        return count == null ? 0L : count;
    }

    /** 移除指定设备上全部登录态（踢下线），返回被清除的会话数。 */
    @Transactional
    public int kickDeviceSessions(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return 0;
        }
        return em.createQuery("delete from AppSessionEntity s where s.fingerprint = :fp")
                .setParameter("fp", fingerprint)
                .executeUpdate();
    }

    /** 清理已过期的 C 端会话。 */
    @Transactional
    public int purgeExpiredAppSessions() {
        return em.createQuery("delete from AppSessionEntity s where s.expireAt < :now")
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
    }

    /** C 端会话表是否为空（用于演示数据初始化）。 */
    public boolean appSessionsEmpty() {
        Long count = em.createQuery("select count(s) from AppSessionEntity s", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void saveAppSessions(List<AppSessionEntity> sessions) {
        for (AppSessionEntity session : sessions) {
            em.persist(session);
        }
    }

    /** 会员套餐。 */
    public List<MembershipPlan> findPlans() {
        return em.createQuery("select p from MembershipPlanEntity p order by p.sortOrder",
                MembershipPlanEntity.class).getResultList().stream()
                .map(p -> new MembershipPlan(p.getName(), p.getPrice(), p.getDesc(), p.getQuota(),
                        p.getSubscribers(), p.getUsage()))
                .toList();
    }

    /** 按主键查找套餐。 */
    public MembershipPlan findPlan(String name) {
        MembershipPlanEntity entity = em.find(MembershipPlanEntity.class, name);
        return entity == null ? null
                : new MembershipPlan(entity.getName(), entity.getPrice(), entity.getDesc(), entity.getQuota(),
                        entity.getSubscribers(), entity.getUsage());
    }

    @Transactional
    public void insertPlan(MembershipPlan plan) {
        Integer max = em.createQuery("select max(p.sortOrder) from MembershipPlanEntity p", Integer.class)
                .getSingleResult();
        em.persist(new MembershipPlanEntity(plan.name(), plan.price(), plan.desc(), plan.quota(),
                plan.subscribers(), plan.usage(), max == null ? 0 : max + 1));
    }

    @Transactional
    public void updatePlan(MembershipPlan plan) {
        MembershipPlanEntity entity = em.find(MembershipPlanEntity.class, plan.name());
        if (entity == null) {
            return;
        }
        entity.setPrice(plan.price());
        entity.setDesc(plan.desc());
        entity.setQuota(plan.quota());
        entity.setSubscribers(plan.subscribers());
        entity.setUsage(plan.usage());
        em.merge(entity);
    }

    @Transactional
    public void deletePlan(String name) {
        MembershipPlanEntity entity = em.find(MembershipPlanEntity.class, name);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /* ------------------------------ 管理员 ------------------------------ */

    /** 全部管理员。 */
    public List<AdminUser> findAdmins() {
        return em.createQuery("select u from AdminUserEntity u order by u.id", AdminUserEntity.class)
                .getResultList().stream().map(this::toAdminModel).toList();
    }

    /** 按主键查找管理员。 */
    public AdminUser findAdmin(Long id) {
        AdminUserEntity entity = em.find(AdminUserEntity.class, id);
        return entity == null ? null : toAdminModel(entity);
    }

    private AdminUser toAdminModel(AdminUserEntity u) {
        return new AdminUser(u.getId(), u.getName(), u.getRole(), u.getGroup(), u.getPhone(), u.getStatus(),
                u.getLastLogin(), u.getUsername());
    }

    @Transactional
    public void deleteAdmin(Long id) {
        AdminUserEntity entity = em.find(AdminUserEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /** 管理员总数。 */
    public long countAdmins() {
        Long count = em.createQuery("select count(u) from AdminUserEntity u", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 管理员表是否为空。 */
    public boolean adminsEmpty() {
        return countAdmins() == 0L;
    }

    @Transactional
    public void saveAdmins(List<AdminUser> users) {
        users.forEach(this::insertAdmin);
    }

    /* --------------------------- 安全策略开关 --------------------------- */

    /** 安全策略开关（复用 strategy_toggle 表，group = security）。 */
    public List<ToggleItem> findPolicies() {
        return em.createQuery(
                        "select s from StrategyToggleEntity s where s.groupName = :g order by s.sortOrder",
                        StrategyToggleEntity.class)
                .setParameter("g", GROUP_SECURITY)
                .getResultList().stream()
                .map(s -> new ToggleItem(s.getName(), s.isEnabled()))
                .toList();
    }

    @Transactional
    public void updatePolicy(String name, boolean enabled) {
        StrategyToggleEntity entity = em.find(StrategyToggleEntity.class, name);
        if (entity != null) {
            entity.setEnabled(enabled);
            em.merge(entity);
        }
    }

    public boolean policiesEmpty() {
        Long count = em.createQuery(
                        "select count(s) from StrategyToggleEntity s where s.groupName = :g", Long.class)
                .setParameter("g", GROUP_SECURITY)
                .getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void savePolicies(List<ToggleItem> policies) {
        int order = 0;
        for (ToggleItem item : policies) {
            em.persist(new StrategyToggleEntity(item.name(), GROUP_SECURITY, item.enabled(), order++));
        }
    }

    /* ---------------------------- C 端功能开关 ---------------------------- */

    /** 按分组读取开关（复用 strategy_toggle 表）。 */
    public List<ToggleItem> findToggles(String group) {
        return em.createQuery(
                        "select s from StrategyToggleEntity s where s.groupName = :g order by s.sortOrder",
                        StrategyToggleEntity.class)
                .setParameter("g", group)
                .getResultList().stream()
                .map(s -> new ToggleItem(s.getName(), s.isEnabled()))
                .toList();
    }

    /** 指定分组的开关是否为空。 */
    public boolean togglesEmpty(String group) {
        Long count = em.createQuery(
                        "select count(s) from StrategyToggleEntity s where s.groupName = :g", Long.class)
                .setParameter("g", group)
                .getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void saveToggles(String group, List<ToggleItem> items) {
        int order = 0;
        for (ToggleItem item : items) {
            em.persist(new StrategyToggleEntity(item.name(), group, item.enabled(), order++));
        }
    }

    /** 按名称切换开关状态（开关名全局唯一，跨分组生效）。 */
    @Transactional
    public void updateToggle(String name, boolean enabled) {
        StrategyToggleEntity entity = em.find(StrategyToggleEntity.class, name);
        if (entity != null) {
            entity.setEnabled(enabled);
            em.merge(entity);
        }
    }

    /* ------------------------------ 系统配置 ------------------------------ */

    /** 读取设置项（不存在时返回 null）。 */
    public String getSetting(String key) {
        AppSetting setting = em.find(AppSetting.class, key);
        return setting == null ? null : setting.getSettingValue();
    }

    /** 写入设置项（幂等 upsert）。 */
    @Transactional
    public void setSetting(String key, String value) {
        AppSetting setting = em.find(AppSetting.class, key);
        if (setting == null) {
            em.persist(new AppSetting(key, value));
        } else {
            setting.setSettingValue(value);
            em.merge(setting);
        }
    }

    /* ------------------------------ C 端用户 ------------------------------ */

    /** 全部 C 端用户。 */
    public List<AppUser> findAppUsers() {
        return em.createQuery("select u from AppUserEntity u order by u.id", AppUserEntity.class)
                .getResultList().stream().map(this::toAppUserModel).toList();
    }

    /**
     * 按条件分页查询 C 端用户：关键字模糊匹配账号，注册方式 / 会员状态 / 账号状态为精确筛选。
     *
     * @param page 1 基页码
     */
    public List<AppUser> findAppUsers(String keyword, String regSource, String membership, String status,
                                      int page, int size) {
        String where = appUserWhere(keyword, regSource, membership, status);
        TypedQuery<AppUserEntity> query = em.createQuery(
                "select u from AppUserEntity u" + where + " order by u.id", AppUserEntity.class);
        bindAppUserParams(query, keyword, regSource, membership, status);
        int safePage = Math.max(1, page);
        query.setFirstResult((safePage - 1) * Math.max(1, size));
        query.setMaxResults(Math.max(1, size));
        return query.getResultList().stream().map(this::toAppUserModel).toList();
    }

    /** 按条件统计 C 端用户数（与 {@link #findAppUsers(String, String, String, String, int, int)} 同口径）。 */
    public long countAppUsers(String keyword, String regSource, String membership, String status) {
        String where = appUserWhere(keyword, regSource, membership, status);
        Query query = em.createQuery("select count(u) from AppUserEntity u" + where, Long.class);
        bindAppUserParams(query, keyword, regSource, membership, status);
        Long total = (Long) query.getSingleResult();
        return total == null ? 0L : total;
    }

    /** C 端用户可选筛选项取值（去重后升序）。 */
    public List<String> appUserOptions(String field) {
        return switch (field) {
            case "regSource" -> em.createQuery(
                    "select distinct u.regSource from AppUserEntity u where u.regSource is not null order by u.regSource",
                    String.class).getResultList();
            case "membership" -> em.createQuery(
                    "select distinct u.membership from AppUserEntity u where u.membership is not null order by u.membership",
                    String.class).getResultList();
            default -> em.createQuery(
                    "select distinct u.status from AppUserEntity u where u.status is not null order by u.status",
                    String.class).getResultList();
        };
    }

    private static String appUserWhere(String keyword, String regSource, String membership, String status) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        if (hasText(keyword)) {
            where.append(" and (lower(u.account) like lower(:kw) or lower(u.regSource) like lower(:kw))");
        }
        if (hasText(regSource)) {
            where.append(" and u.regSource = :regSource");
        }
        if (hasText(membership)) {
            where.append(" and u.membership = :membership");
        }
        if (hasText(status)) {
            where.append(" and u.status = :status");
        }
        return where.toString();
    }

    private static void bindAppUserParams(Query query, String keyword, String regSource, String membership,
                                          String status) {
        if (hasText(keyword)) {
            query.setParameter("kw", "%" + keyword.trim() + "%");
        }
        if (hasText(regSource)) {
            query.setParameter("regSource", regSource.trim());
        }
        if (hasText(membership)) {
            query.setParameter("membership", membership.trim());
        }
        if (hasText(status)) {
            query.setParameter("status", status.trim());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 按主键查找 C 端用户。 */
    public AppUser findAppUser(Long id) {
        AppUserEntity entity = em.find(AppUserEntity.class, id);
        return entity == null ? null : toAppUserModel(entity);
    }

    /** C 端用户总数。 */
    public long countAppUsers() {
        Long count = em.createQuery("select count(u) from AppUserEntity u", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** C 端用户表是否为空。 */
    public boolean appUsersEmpty() {
        return countAppUsers() == 0L;
    }

    @Transactional
    public void saveAppUsers(List<AppUser> users) {
        users.forEach(u -> em.persist(new AppUserEntity(u.account(), u.regSource(), u.membership(),
                u.registered(), u.lastActive(), u.status())));
    }

    /** 更新 C 端用户（会员状态 / 启停用）。 */
    @Transactional
    public void updateAppUser(AppUser user) {
        AppUserEntity entity = em.find(AppUserEntity.class, user.id());
        if (entity == null) {
            return;
        }
        entity.setMembership(user.membership());
        entity.setStatus(user.status());
        em.merge(entity);
    }

    private AppUser toAppUserModel(AppUserEntity u) {
        return new AppUser(u.getId(), u.getAccount(), u.getRegSource(), u.getMembership(), u.getRegistered(),
                u.getLastActive(), u.getStatus());
    }

    /* ------------------------------ 审计日志 ------------------------------ */

    /**
     * 操作日志分页（page 从 1 开始，可按 yyyy-MM-dd 时间范围过滤）。
     * 过滤条件下推到数据库执行，避免把全部日志读进内存。
     */
    public List<AuditLogEntry> findAuditLogs(int page, int size, String start, String end) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        TypedQuery<AuditLogEntity> query = em
                .createQuery("select l from AuditLogEntity l" + auditRangeClause(start, end) + " order by l.id desc",
                        AuditLogEntity.class)
                .setFirstResult((safePage - 1) * safeSize)
                .setMaxResults(safeSize);
        bindAuditRange(query, start, end);
        return query.getResultList().stream().map(this::toLogModel).toList();
    }

    /** 操作日志按天计数（key = yyyy-MM-dd，升序），用于趋势迷你图。 */
    public Map<String, Long> countLogsByDate(String start, String end) {
        Query query = em.createQuery(
                        "select substring(l.time, 1, 10), count(l) from AuditLogEntity l"
                                + auditRangeClause(start, end)
                                + " group by substring(l.time, 1, 10) order by substring(l.time, 1, 10)",
                        Object[].class);
        bindAuditRange(query, start, end);
        List<Object[]> rows = query.getResultList();
        Map<String, Long> out = new LinkedHashMap<>();
        for (Object[] row : rows) {
            out.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return out;
    }

    /** 追加一条操作日志（未指定哈希时自动生成，模拟区块链存证）。 */
    @Transactional
    public void pushAuditLog(AuditLogEntry entry) {
        String hash = entry.hash() == null || entry.hash().isBlank()
                ? UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : entry.hash();
        em.persist(new AuditLogEntity(entry.time(), entry.operator(), entry.role(), entry.group(), entry.action(),
                entry.detail(), entry.source(), entry.result(), hash));
        trimAuditLogs();
    }

    /** 全球登录监控点位。 */
    public List<MonitorPoint> findPoints() {
        return em.createQuery("select p from MonitorPointEntity p order by p.sortOrder", MonitorPointEntity.class)
                .getResultList().stream()
                .map(p -> new MonitorPoint(p.getName(), p.getLon(), p.getLat(), p.getSessions(), p.getLevel()))
                .toList();
    }

    /** 操作日志总量（可按 yyyy-MM-dd 时间范围过滤，数据库侧 count）。 */
    public long totalLogs(String start, String end) {
        Query query = em.createQuery("select count(l) from AuditLogEntity l" + auditRangeClause(start, end),
                Long.class);
        bindAuditRange(query, start, end);
        Long count = (Long) query.getSingleResult();
        return count == null ? 0L : count;
    }

    /**
     * 时间范围过滤条件：日志时间以 yyyy-MM-dd 开头，取前 10 位做字符串比较即等价于日期比较。
     * 仅拼接条件骨架，日期一律以参数绑定，无 SQL 注入风险。
     */
    private static String auditRangeClause(String start, String end) {
        StringBuilder clause = new StringBuilder(" where 1=1");
        if (start != null && !start.isBlank()) {
            clause.append(" and substring(l.time, 1, 10) >= :start");
        }
        if (end != null && !end.isBlank()) {
            clause.append(" and substring(l.time, 1, 10) <= :end");
        }
        return clause.toString();
    }

    private static void bindAuditRange(Query query, String start, String end) {
        if (start != null && !start.isBlank()) {
            query.setParameter("start", start);
        }
        if (end != null && !end.isBlank()) {
            query.setParameter("end", end);
        }
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(r) from RoleEntity r", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    /** 一次性迁移：重命名管理员显示名（幂等，执行后不再有匹配行）。 */
    @Transactional
    public void renameAdmin(String fromName, String toName) {
        em.createQuery("update AdminUserEntity u set u.name = :to where u.name = :from")
                .setParameter("to", toName)
                .setParameter("from", fromName)
                .executeUpdate();
    }

    @Transactional
    public void saveRoles(List<RoleDomain> roles) {
        insertRoles(roles);
    }

    /** 新增管理员：账号缺省按姓名生成，口令使用默认口令（首次登录后可自行修改）。 */
    @Transactional
    public void insertAdmin(AdminUser user) {
        String username = (user.username() == null || user.username().isBlank())
                ? "admin" + System.currentTimeMillis()
                : user.username().trim();
        em.persist(new AdminUserEntity(user.name(), user.role(), user.group(), user.phone(), user.status(),
                user.lastLogin(), username, PasswordHasher.hash(DEFAULT_PASSWORD)));
    }

    /**
     * 幂等刷新管理员账号：同名记录存在则补填缺失的账号 / 口令（不覆盖已有口令），
     * 不存在则新增。用于首次启动或老数据缺账号字段时的兼容修复。
     */
    @Transactional
    public void refreshAdmin(AdminUser seed) {
        AdminUserEntity entity = em.createQuery(
                        "select u from AdminUserEntity u where u.name = :n", AdminUserEntity.class)
                .setParameter("n", seed.name()).setMaxResults(1).getResultList().stream().findFirst()
                .orElse(null);
        if (entity == null) {
            em.persist(new AdminUserEntity(seed.name(), seed.role(), seed.group(), seed.phone(),
                    seed.status(), seed.lastLogin(), seed.username(),
                    PasswordHasher.hash(DEFAULT_PASSWORD)));
        } else {
            if (entity.getUsername() == null || entity.getUsername().isBlank()) {
                entity.setUsername(seed.username());
            }
            if (entity.getPasswordHash() == null || entity.getPasswordHash().isBlank()) {
                entity.setPasswordHash(PasswordHasher.hash(DEFAULT_PASSWORD));
            }
            em.merge(entity);
        }
    }

    /** 更新管理员（保持账号与口令不变，口令修改走独立接口）。 */
    @Transactional
    public void updateAdmin(AdminUser user) {
        AdminUserEntity entity = em.find(AdminUserEntity.class, user.id());
        if (entity == null) {
            return;
        }
        entity.setName(user.name());
        entity.setRole(user.role());
        entity.setGroup(user.group());
        entity.setPhone(user.phone());
        entity.setStatus(user.status());
        entity.setLastLogin(user.lastLogin());
        if (user.username() != null && !user.username().isBlank()) {
            entity.setUsername(user.username().trim());
        }
        em.merge(entity);
    }

    @Transactional
    public void saveDevices(List<DeviceRecord> devices) {
        int order = 0;
        for (DeviceRecord device : devices) {
            em.persist(new DeviceEntity(device.fingerprint(), device.region(), device.ip(), device.sessions(),
                    device.risk(), device.verdict(), device.banned(), device.account(), order++));
        }
    }

    @Transactional
    public void savePlans(List<MembershipPlan> plans) {
        int order = 0;
        for (MembershipPlan plan : plans) {
            em.persist(new MembershipPlanEntity(plan.name(), plan.price(), plan.desc(), plan.quota(),
                    plan.subscribers(), plan.usage(), order++));
        }
    }

    @Transactional
    public void saveAuditLogs(List<AuditLogEntry> logs) {
        // 列表按"最新在前"传入，落库时反转，保证 id 升序即时间正序
        for (int i = logs.size() - 1; i >= 0; i--) {
            AuditLogEntry log = logs.get(i);
            em.persist(new AuditLogEntity(log.time(), log.operator(), log.role(), log.group(), log.action(),
                    log.detail(), log.source(), log.result(), log.hash()));
        }
    }

    @Transactional
    public void savePoints(List<MonitorPoint> points) {
        int order = 0;
        for (MonitorPoint point : points) {
            em.persist(new MonitorPointEntity(point.name(), point.lon(), point.lat(), point.sessions(),
                    point.level(), order++));
        }
    }

    private AuditLogEntry toLogModel(AuditLogEntity l) {
        return new AuditLogEntry(l.getId(), l.getTime(), l.getOperator(), l.getRole(), l.getGroup(), l.getAction(),
                l.getDetail(), l.getSource(), l.getResult(), l.getHash());
    }

    /** 仅保留最近 200 条日志，避免演示数据无限增长。 */
    private void trimAuditLogs() {
        List<Long> ids = em.createQuery("select l.id from AuditLogEntity l order by l.id desc", Long.class)
                .getResultList();
        for (int i = 200; i < ids.size(); i++) {
            AuditLogEntity stale = em.find(AuditLogEntity.class, ids.get(i));
            if (stale != null) {
                em.remove(stale);
            }
        }
    }

    private RoleDomain toRoleModel(RoleEntity entity) {
        List<PermGroup> groups = new ArrayList<>();
        entity.getGroups().forEach(g -> {
            List<Permission> permissions = new ArrayList<>();
            g.getPermissions().forEach(p -> permissions.add(new Permission(p.getName(), p.getState())));
            groups.add(new PermGroup(g.getName(), g.getMembers(), permissions));
        });
        return new RoleDomain(entity.getName(), entity.getCode(), entity.getIcon(), entity.getMembers(), groups);
    }
}
