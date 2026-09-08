package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.dto.common.ToggleItem;
import com.gzu.adminconsole.entity.AppSetting;
import com.gzu.adminconsole.entity.ModelReleaseEntity;
import com.gzu.adminconsole.entity.ReleaseEventEntity;
import com.gzu.adminconsole.entity.StrategyToggleEntity;
import com.gzu.adminconsole.model.ModelRelease;
import com.gzu.adminconsole.model.ReleaseEvent;

/**
 * AI 模型与灰度发布数据访问层（JPA 实现）。
 */
@Repository
@Transactional(readOnly = true)
public class ModelRepository {

    /** 设置项：全局灰度比例。 */
    public static final String KEY_GRAY_RATIO = "models.grayscale.ratio";
    /** 设置项：近 7 日自动回滚次数。 */
    public static final String KEY_ROLLBACK_COUNT = "models.autoRollback.count";
    /** 开关分组：模型热更策略。 */
    public static final String GROUP_MODELS = "models";

    @PersistenceContext
    private EntityManager em;

    /** 全部模型版本。 */
    public List<ModelRelease> findModels() {
        return em.createQuery("select m from ModelReleaseEntity m order by m.name", ModelReleaseEntity.class)
                .getResultList().stream().map(this::toModel).toList();
    }

    /** 按名称查找模型。 */
    public ModelRelease findModel(String name) {
        ModelReleaseEntity entity = em.find(ModelReleaseEntity.class, name);
        return entity == null ? null : toModel(entity);
    }

    /** 更新模型（整体替换）。 */
    @Transactional
    public void updateModel(ModelRelease target) {
        ModelReleaseEntity entity = em.find(ModelReleaseEntity.class, target.name());
        if (entity == null) {
            return;
        }
        entity.setType(target.type());
        entity.setPrecision(target.precision());
        entity.setSize(target.size());
        entity.setCoverage(target.coverage());
        entity.setGrayRatio(target.grayRatio());
        entity.setStatus(target.status());
        em.merge(entity);
    }

    /** 新增模型版本。 */
    @Transactional
    public void insertModel(ModelRelease model) {
        em.persist(new ModelReleaseEntity(model.name(), model.type(), model.precision(), model.size(),
                model.coverage(), model.grayRatio(), model.status()));
    }

    /** 删除模型版本。 */
    @Transactional
    public void deleteModel(String name) {
        ModelReleaseEntity entity = em.find(ModelReleaseEntity.class, name);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /** 切换策略开关。 */
    @Transactional
    public void updateStrategy(String name, boolean enabled) {
        StrategyToggleEntity entity = em.find(StrategyToggleEntity.class, name);
        if (entity != null) {
            entity.setEnabled(enabled);
            em.merge(entity);
        }
    }

    /** 版本迭代时间线（最新在前）。 */
    public List<ReleaseEvent> findTimeline() {
        return em.createQuery("select e from ReleaseEventEntity e order by e.id desc", ReleaseEventEntity.class)
                .getResultList().stream()
                .map(e -> new ReleaseEvent(e.getTitle(), e.getTime(), e.getDesc(), e.getTone()))
                .toList();
    }

    /** 追加一条时间线事件。 */
    @Transactional
    public void pushEvent(ReleaseEvent event) {
        em.persist(new ReleaseEventEntity(event.title(), event.time(), event.desc(), event.tone()));
        trimTimeline();
    }

    /** 灰度下发与热更策略开关。 */
    public List<ToggleItem> findStrategies() {
        return em.createQuery(
                        "select s from StrategyToggleEntity s where s.groupName = :g order by s.sortOrder",
                        StrategyToggleEntity.class)
                .setParameter("g", GROUP_MODELS)
                .getResultList().stream()
                .map(s -> new ToggleItem(s.getName(), s.isEnabled()))
                .toList();
    }

    public int getGrayscaleRatio() {
        return Integer.parseInt(readSetting(KEY_GRAY_RATIO, "35"));
    }

    @Transactional
    public void setGrayscaleRatio(int grayscaleRatio) {
        writeSetting(KEY_GRAY_RATIO, String.valueOf(grayscaleRatio));
    }

    public int getAutoRollbackCount() {
        return Integer.parseInt(readSetting(KEY_ROLLBACK_COUNT, "3"));
    }

    @Transactional
    public void increaseAutoRollback() {
        writeSetting(KEY_ROLLBACK_COUNT, String.valueOf(getAutoRollbackCount() + 1));
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(m) from ModelReleaseEntity m", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void saveSettings(int grayRatio, int rollbackCount) {
        writeSetting(KEY_GRAY_RATIO, String.valueOf(grayRatio));
        writeSetting(KEY_ROLLBACK_COUNT, String.valueOf(rollbackCount));
    }

    @Transactional
    public void saveModels(List<ModelRelease> models) {
        models.forEach(m -> em.persist(new ModelReleaseEntity(m.name(), m.type(), m.precision(), m.size(),
                m.coverage(), m.grayRatio(), m.status())));
    }

    @Transactional
    public void saveTimeline(List<ReleaseEvent> events) {
        // 列表按"最新在前"传入，落库时反转，保证 id 升序即时间正序
        for (int i = events.size() - 1; i >= 0; i--) {
            ReleaseEvent event = events.get(i);
            em.persist(new ReleaseEventEntity(event.title(), event.time(), event.desc(), event.tone()));
        }
    }

    @Transactional
    public void saveStrategies(List<ToggleItem> strategies) {
        int order = 0;
        for (ToggleItem item : strategies) {
            em.persist(new StrategyToggleEntity(item.name(), GROUP_MODELS, item.enabled(), order++));
        }
    }

    /** 仅保留最近 12 条时间线，避免无限增长。 */
    private void trimTimeline() {
        List<Long> ids = em.createQuery("select e.id from ReleaseEventEntity e order by e.id desc", Long.class)
                .getResultList();
        for (int i = 12; i < ids.size(); i++) {
            ReleaseEventEntity stale = em.find(ReleaseEventEntity.class, ids.get(i));
            if (stale != null) {
                em.remove(stale);
            }
        }
    }

    private String readSetting(String key, String fallback) {
        AppSetting setting = em.find(AppSetting.class, key);
        return setting == null ? fallback : setting.getSettingValue();
    }

    private void writeSetting(String key, String value) {
        AppSetting setting = em.find(AppSetting.class, key);
        if (setting == null) {
            em.persist(new AppSetting(key, value));
        } else {
            setting.setSettingValue(value);
            em.merge(setting);
        }
    }

    private ModelRelease toModel(ModelReleaseEntity e) {
        return new ModelRelease(e.getName(), e.getType(), e.getPrecision(), e.getSize(), e.getCoverage(),
                e.getGrayRatio(), e.getStatus());
    }
}
