package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantGoodsEntity;

/**
 * 商户商品数据访问层（模块自有，只操作 merchant_goods 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantGoodsRepository {

    @PersistenceContext
    private EntityManager em;

    public List<MerchantGoodsEntity> findAll() {
        return em.createQuery("select g from MerchantGoodsEntity g order by g.id", MerchantGoodsEntity.class)
                .getResultList();
    }

    public MerchantGoodsEntity findById(Long id) {
        return em.find(MerchantGoodsEntity.class, id);
    }

    public long count() {
        Long count = em.createQuery("select count(g) from MerchantGoodsEntity g", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantGoodsEntity save(MerchantGoodsEntity entity) {
        return em.merge(entity);
    }

    @Transactional
    public void delete(MerchantGoodsEntity entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
}
