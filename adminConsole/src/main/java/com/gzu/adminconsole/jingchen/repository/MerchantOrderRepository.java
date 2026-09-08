package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantOrderEntity;

/**
 * 商户订单数据访问层（模块自有，只操作 merchant_order 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantOrderRepository {

    @PersistenceContext
    private EntityManager em;

    /** 全部订单,按下单时间倒序(最新在前)。 */
    public List<MerchantOrderEntity> findAll() {
        return em.createQuery(
                        "select o from MerchantOrderEntity o order by o.createdAt desc, o.id desc",
                        MerchantOrderEntity.class)
                .getResultList();
    }

    /** 按主键查找。 */
    public MerchantOrderEntity findById(Long id) {
        return em.find(MerchantOrderEntity.class, id);
    }

    /** 订单条数。 */
    public long count() {
        Long count = em.createQuery("select count(o) from MerchantOrderEntity o", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 按结算状态统计条数。 */
    public long countByStatus(String status) {
        Long count = em.createQuery("select count(o) from MerchantOrderEntity o where o.status = :s", Long.class)
                .setParameter("s", status)
                .getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public void save(MerchantOrderEntity entity) {
        em.persist(entity);
    }

    @Transactional
    public MerchantOrderEntity update(MerchantOrderEntity entity) {
        return em.merge(entity);
    }
}
