package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantPlanEntity;

/**
 * 商户投放计划数据访问层（模块自有，只操作 ad_plan 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantPlanRepository {

    @PersistenceContext
    private EntityManager em;

    /** 全部投放计划,按 id 升序。 */
    public List<MerchantPlanEntity> findAll() {
        return em.createQuery("select p from MerchantPlanEntity p order by p.id", MerchantPlanEntity.class)
                .getResultList();
    }

    /** 按主键查找。 */
    public MerchantPlanEntity findById(Long id) {
        return em.find(MerchantPlanEntity.class, id);
    }

    /** 计划条数(判断是否需要灌入演示数据)。 */
    public long count() {
        Long count = em.createQuery("select count(p) from MerchantPlanEntity p", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 新增或更新(merge 兼容两种情况),返回托管后的实体。 */
    @Transactional
    public MerchantPlanEntity save(MerchantPlanEntity plan) {
        return em.merge(plan);
    }

    /** 删除计划。 */
    @Transactional
    public void delete(MerchantPlanEntity plan) {
        em.remove(em.contains(plan) ? plan : em.merge(plan));
    }
}
