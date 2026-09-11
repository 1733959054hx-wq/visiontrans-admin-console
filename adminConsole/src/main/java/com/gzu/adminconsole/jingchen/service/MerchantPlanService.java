package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantPlanRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantPlanEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantPlanRepository;

/**
 * 商户投放计划业务逻辑（模块自有）。
 *
 * <p>状态机约定:「投放中 / 预算预警 / 待审核」由系统维护,前端编辑只允许
 * 修改名称、形式、场景、预算、负责人与 CTR;已消耗与暂停状态不可经编辑接口变更,
 * 防止绕过业务规则(与后台管理模块口径一致)。
 */
@Service
public class MerchantPlanService {

    private final MerchantPlanRepository repository;

    public MerchantPlanService(MerchantPlanRepository repository) {
        this.repository = repository;
    }

    /** 计划列表。 */
    public List<MerchantPlanEntity> list() {
        return repository.findAll();
    }

    /** 按主键查找,不存在抛 404。 */
    public MerchantPlanEntity get(Long id) {
        MerchantPlanEntity plan = repository.findById(id);
        if (plan == null) {
            throw new BusinessException(404, "投放计划不存在: id=" + id);
        }
        return plan;
    }

    /** 新建:编号自动生成,已消耗从 0 起算,默认投放中;带投放设置(广告位 / 时段 / 人群)。 */
    public MerchantPlanEntity create(MerchantPlanRequest req) {
        MerchantPlanEntity plan = new MerchantPlanEntity(
                req.planNo() == null || req.planNo().isBlank()
                        ? "PLAN-" + (System.currentTimeMillis() % 1_000_000)
                        : req.planNo().trim(),
                requireName(req.name()),
                req.adForm(),
                req.scene(),
                req.budget(),
                BigDecimal.ZERO,
                "投放中",
                req.owner(),
                req.ctr(),
                false);
        plan.setSlotName(req.slotName());
        plan.setTimeRange(req.timeRange());
        plan.setTargeting(req.targeting());
        return repository.save(plan);
    }

    /** 编辑:只覆盖请求中携带的非空字段,已消耗与暂停状态不允许修改。 */
    public MerchantPlanEntity update(Long id, MerchantPlanRequest req) {
        MerchantPlanEntity plan = get(id);
        if (req.name() != null && !req.name().isBlank()) plan.setName(req.name().trim());
        if (req.adForm() != null) plan.setAdForm(req.adForm());
        if (req.scene() != null) plan.setScene(req.scene());
        if (req.budget() != null) plan.setBudget(req.budget());
        if (req.owner() != null) plan.setOwner(req.owner());
        if (req.ctr() != null) plan.setCtr(req.ctr());
        if (req.slotName() != null) plan.setSlotName(req.slotName());
        if (req.timeRange() != null) plan.setTimeRange(req.timeRange());
        if (req.targeting() != null) plan.setTargeting(req.targeting());
        return repository.save(plan);
    }

    /** 可用广告位列表(主工程 ad_slot 库存,仅上线位,供「广告位选择」)。 */
    @Transactional(readOnly = true)
    public List<com.gzu.adminconsole.entity.AdSlotEntity> listSlots() {
        return repository.findOnlineSlots();
    }

    /** 暂停 / 恢复:状态与暂停标记同步维护。 */
    public MerchantPlanEntity toggle(Long id, boolean pause) {
        MerchantPlanEntity plan = get(id);
        plan.setPaused(pause);
        plan.setStatus(pause ? "已暂停" : "投放中");
        return repository.save(plan);
    }

    /** 删除计划。 */
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "计划名称必填");
        }
        return name.trim();
    }
}
