package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantAbConfigEntity;

/**
 * 商户 A/B 测试配置数据访问层（模块自有，单行配置,只操作 merchant_ab_config 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantAbConfigRepository {

    @PersistenceContext
    private EntityManager em;

    /** 取单行配置;未初始化时返回 null。 */
    public MerchantAbConfigEntity findFirst() {
        List<MerchantAbConfigEntity> list = em.createQuery(
                        "select c from MerchantAbConfigEntity c order by c.id", MerchantAbConfigEntity.class)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public long count() {
        Long count = em.createQuery("select count(c) from MerchantAbConfigEntity c", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantAbConfigEntity save(MerchantAbConfigEntity entity) {
        return em.merge(entity);
    }
}
