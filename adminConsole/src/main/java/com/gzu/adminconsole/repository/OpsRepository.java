package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.AppSetting;
import com.gzu.adminconsole.entity.BackupPolicyEntity;
import com.gzu.adminconsole.entity.CircuitBreakerEntity;
import com.gzu.adminconsole.entity.SystemLogEntity;

/**
 * 监控运维数据访问层：系统日志、熔断降级、备份策略与告警阈值（JPA 实现）。
 */
@Repository
@Transactional(readOnly = true)
public class OpsRepository {

    /** 阈值设置键：CPU 利用率告警阈值（%）。 */
    public static final String KEY_THRESHOLD_CPU = "ops.threshold.cpu";
    /** 阈值设置键：内存利用率告警阈值（%）。 */
    public static final String KEY_THRESHOLD_MEM = "ops.threshold.mem";
    /** 阈值设置键：GPU 利用率告警阈值（%）。 */
    public static final String KEY_THRESHOLD_GPU = "ops.threshold.gpu";

    @PersistenceContext
    private EntityManager em;

    /* ------------------------------ 系统日志 ------------------------------ */

    /**
     * 系统日志（level / category 可空，空表示不过滤）。
     *
     * @param level    级别：INFO / WARN / ERROR，可空
     * @param category 分类：应用日志 / 错误日志 / 模型推理，可空
     */
    public List<SystemLogEntity> findSysLogs(String level, String category) {
        StringBuilder ql = new StringBuilder("select l from SystemLogEntity l where 1=1");
        if (level != null && !level.isBlank()) {
            ql.append(" and l.level = :level");
        }
        if (category != null && !category.isBlank()) {
            ql.append(" and l.category = :category");
        }
        var query = em.createQuery(ql.append(" order by l.sortOrder").toString(), SystemLogEntity.class);
        if (level != null && !level.isBlank()) {
            query.setParameter("level", level);
        }
        if (category != null && !category.isBlank()) {
            query.setParameter("category", category);
        }
        return query.getResultList();
    }

    /** 系统日志条数。 */
    public long countSysLogs() {
        Long count = em.createQuery("select count(l) from SystemLogEntity l", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public void saveSysLogs(List<SystemLogEntity> logs) {
        int order = 0;
        for (SystemLogEntity log : logs) {
            em.persist(new SystemLogEntity(log.getTime(), log.getLevel(), log.getCategory(), log.getSource(),
                    log.getMessage(), order++));
        }
    }

    /* ------------------------------ 熔断降级 ------------------------------ */

    /** 全部熔断降级策略。 */
    public List<CircuitBreakerEntity> findBreakers() {
        return em.createQuery("select b from CircuitBreakerEntity b order by b.sortOrder",
                CircuitBreakerEntity.class).getResultList();
    }

    /** 按主键查找熔断降级策略。 */
    public CircuitBreakerEntity findBreaker(Long id) {
        return em.find(CircuitBreakerEntity.class, id);
    }

    /** 更新熔断降级策略（状态 + 启用）。 */
    @Transactional
    public void updateBreaker(CircuitBreakerEntity breaker) {
        em.merge(breaker);
    }

    /** 熔断策略条数。 */
    public long countBreakers() {
        Long count = em.createQuery("select count(b) from CircuitBreakerEntity b", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public void saveBreakers(List<CircuitBreakerEntity> breakers) {
        int order = 0;
        for (CircuitBreakerEntity breaker : breakers) {
            em.persist(new CircuitBreakerEntity(breaker.getService(), breaker.getStrategy(), breaker.getThreshold(),
                    breaker.getState(), breaker.isEnabled(), order++));
        }
    }

    /* ------------------------------ 备份策略 ------------------------------ */

    /** 全部备份策略。 */
    public List<BackupPolicyEntity> findBackups() {
        return em.createQuery("select b from BackupPolicyEntity b order by b.sortOrder",
                BackupPolicyEntity.class).getResultList();
    }

    /** 按主键查找备份策略。 */
    public BackupPolicyEntity findBackup(Long id) {
        return em.find(BackupPolicyEntity.class, id);
    }

    /** 更新备份策略（启用状态）。 */
    @Transactional
    public void updateBackup(BackupPolicyEntity backup) {
        em.merge(backup);
    }

    /** 备份策略条数。 */
    public long countBackups() {
        Long count = em.createQuery("select count(b) from BackupPolicyEntity b", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public void saveBackups(List<BackupPolicyEntity> backups) {
        int order = 0;
        for (BackupPolicyEntity backup : backups) {
            em.persist(new BackupPolicyEntity(backup.getTarget(), backup.getCycle(), backup.getRetention(),
                    backup.getStorage(), backup.isEnabled(), order++));
        }
    }

    /* ------------------------------ 告警阈值 ------------------------------ */

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
}
