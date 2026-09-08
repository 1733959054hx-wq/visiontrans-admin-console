package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantDailyStatEntity;

/**
 * 商户经营日统计数据访问层（模块自有，只操作 merchant_daily_stat 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantStatRepository {

    @PersistenceContext
    private EntityManager em;

    /** 按日期升序返回全部日统计。 */
    public List<MerchantDailyStatEntity> findAll() {
        return em.createQuery(
                        "select s from MerchantDailyStatEntity s order by s.statDate", MerchantDailyStatEntity.class)
                .getResultList();
    }

    /** 统计条数(判断是否需要灌入演示数据)。 */
    public long count() {
        Long count = em.createQuery("select count(s) from MerchantDailyStatEntity s", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public void save(MerchantDailyStatEntity entity) {
        em.persist(entity);
    }
}
