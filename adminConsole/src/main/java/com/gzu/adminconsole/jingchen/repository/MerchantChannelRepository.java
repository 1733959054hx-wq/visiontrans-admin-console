package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantChannelEntity;

/**
 * 商户推广渠道数据访问层（模块自有，只操作 merchant_channel 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantChannelRepository {

    @PersistenceContext
    private EntityManager em;

    public List<MerchantChannelEntity> findAll() {
        return em.createQuery("select c from MerchantChannelEntity c order by c.id", MerchantChannelEntity.class)
                .getResultList();
    }

    public MerchantChannelEntity findById(Long id) {
        return em.find(MerchantChannelEntity.class, id);
    }

    public long count() {
        Long count = em.createQuery("select count(c) from MerchantChannelEntity c", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantChannelEntity save(MerchantChannelEntity entity) {
        return em.merge(entity);
    }

    @Transactional
    public void delete(MerchantChannelEntity entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
}
