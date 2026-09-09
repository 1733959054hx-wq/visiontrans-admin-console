package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.dto.common.ToggleItem;
import com.gzu.adminconsole.entity.AdSlotEntity;
import com.gzu.adminconsole.entity.AppSetting;
import com.gzu.adminconsole.entity.FrequencyCapEntity;
import com.gzu.adminconsole.entity.StrategyToggleEntity;
import com.gzu.adminconsole.model.AdSlot;
import com.gzu.adminconsole.model.FrequencyCap;

/**
 * 广告位排期与调度数据访问层（JPA 实现）。
 */
@Repository
@Transactional(readOnly = true)
public class AdRepository {

    /** 设置项：AI 建议是否已采纳。 */
    public static final String KEY_ADVICE_ADOPTED = "ads.advice.adopted";
    /** 开关分组：频控附加策略。 */
    public static final String GROUP_ADS = "ads";

    /** 甘特图排期日期（相对固定的展示维度，不落库）。 */
    private final List<String> days = List.of("09-03", "09-04", "09-05", "09-06", "09-07", "09-08", "09-09");
    /** 投放场景。 */
    private final List<String> scenes = List.of("街景", "商超", "机场", "会展", "校园", "影院", "景区");
    /** 语种对。 */
    private final List<String> langs = List.of("EN↔ZH", "JA↔ZH", "KO↔ZH", "DE↔ZH", "FR↔ZH", "ES↔ZH", "AR↔ZH");

    @PersistenceContext
    private EntityManager em;

    /** 甘特图排期日期（可按 yyyy-MM-dd 范围过滤，短格式补当前年）。 */
    public List<String> findDays(String start, String end) {
        return days.stream().filter(d -> DateRange.inRange(d, start, end)).toList();
    }

    public List<String> findScenes() {
        return scenes;
    }

    public List<String> findLangs() {
        return langs;
    }

    /** 全部广告位。 */
    public List<AdSlot> findSlots() {
        return em.createQuery("select s from AdSlotEntity s order by s.sortOrder", AdSlotEntity.class)
                .getResultList().stream().map(this::toSlotModel).toList();
    }

    /** 按主键查找广告位。 */
    public AdSlot findSlot(Long id) {
        AdSlotEntity entity = em.find(AdSlotEntity.class, id);
        return entity == null ? null : toSlotModel(entity);
    }

    /** 新增广告位。 */
    @Transactional
    public void insertSlot(AdSlot slot) {
        Integer max = em.createQuery("select max(s.sortOrder) from AdSlotEntity s", Integer.class).getSingleResult();
        em.persist(new AdSlotEntity(slot.name(), slot.status(), slot.color(), slot.remain(), slot.ratio(),
                max == null ? 0 : max + 1));
    }

    /** 更新广告位。 */
    @Transactional
    public void updateSlot(AdSlot slot) {
        AdSlotEntity entity = em.find(AdSlotEntity.class, slot.id());
        if (entity == null) {
            return;
        }
        entity.setName(slot.name());
        entity.setStatus(slot.status());
        entity.setColor(slot.color());
        entity.setRemain(slot.remain());
        entity.setRatio(slot.ratio());
        em.merge(entity);
    }

    /** 删除广告位。 */
    @Transactional
    public void deleteSlot(Long id) {
        AdSlotEntity entity = em.find(AdSlotEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /** 上线 / 下线广告位（幂等覆盖）。 */
    @Transactional
    public void updateSlotOnline(Long id, boolean online) {
        AdSlotEntity entity = em.find(AdSlotEntity.class, id);
        if (entity == null) {
            return;
        }
        entity.setOnline(online);
        em.merge(entity);
    }

    /** 全部频控项。 */
    public List<FrequencyCap> findCaps() {
        return em.createQuery("select c from FrequencyCapEntity c order by c.sortOrder", FrequencyCapEntity.class)
                .getResultList().stream()
                .map(c -> new FrequencyCap(c.getName(), c.getDisplay(), c.getMaxValue(), c.getValue()))
                .toList();
    }

    /** 按名称查找频控项。 */
    public FrequencyCap findCap(String name) {
        FrequencyCapEntity entity = em.find(FrequencyCapEntity.class, name);
        return entity == null ? null
                : new FrequencyCap(entity.getName(), entity.getDisplay(), entity.getMaxValue(), entity.getValue());
    }

    /** 更新频控项。 */
    @Transactional
    public void updateCap(FrequencyCap cap) {
        FrequencyCapEntity entity = em.find(FrequencyCapEntity.class, cap.name());
        if (entity == null) {
            return;
        }
        entity.setValue(cap.value());
        em.merge(entity);
    }

    /** 频控附加策略开关。 */
    public List<ToggleItem> findFreqStrategies() {
        return em.createQuery(
                        "select s from StrategyToggleEntity s where s.groupName = :g order by s.sortOrder",
                        StrategyToggleEntity.class)
                .setParameter("g", GROUP_ADS)
                .getResultList().stream()
                .map(s -> new ToggleItem(s.getName(), s.isEnabled()))
                .toList();
    }

    public boolean isAdviceAdopted() {
        AppSetting setting = em.find(AppSetting.class, KEY_ADVICE_ADOPTED);
        return setting != null && Boolean.parseBoolean(setting.getSettingValue());
    }

    @Transactional
    public void setAdviceAdopted(boolean adopted) {
        AppSetting setting = em.find(AppSetting.class, KEY_ADVICE_ADOPTED);
        if (setting == null) {
            em.persist(new AppSetting(KEY_ADVICE_ADOPTED, String.valueOf(adopted)));
        } else {
            setting.setSettingValue(String.valueOf(adopted));
            em.merge(setting);
        }
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(s) from AdSlotEntity s", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void saveSlots(List<AdSlot> slots) {
        int order = 0;
        for (AdSlot slot : slots) {
            em.persist(new AdSlotEntity(slot.name(), slot.status(), slot.color(), slot.remain(), slot.ratio(),
                    order++));
        }
    }

    @Transactional
    public void saveCaps(List<FrequencyCap> caps) {
        int order = 0;
        for (FrequencyCap cap : caps) {
            em.persist(new FrequencyCapEntity(cap.name(), cap.display(), cap.max(), cap.value(), order++));
        }
    }

    @Transactional
    public void saveFreqStrategies(List<ToggleItem> strategies) {
        int order = 0;
        for (ToggleItem item : strategies) {
            em.persist(new StrategyToggleEntity(item.name(), GROUP_ADS, item.enabled(), order++));
        }
    }

    private AdSlot toSlotModel(AdSlotEntity s) {
        return new AdSlot(s.getId(), s.getName(), s.getStatus(), s.getColor(), s.getRemain(), s.getRatio(),
                s.getOnline() == null || s.getOnline());
    }
}
