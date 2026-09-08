package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;

/**
 * 商户视频数据访问层（模块自有，只操作 merchant_video 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantVideoRepository {

    @PersistenceContext
    private EntityManager em;

    public List<MerchantVideoEntity> findAll() {
        return em.createQuery("select v from MerchantVideoEntity v order by v.id", MerchantVideoEntity.class)
                .getResultList();
    }

    public MerchantVideoEntity findById(Long id) {
        return em.find(MerchantVideoEntity.class, id);
    }

    public long count() {
        Long count = em.createQuery("select count(v) from MerchantVideoEntity v", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantVideoEntity save(MerchantVideoEntity entity) {
        return em.merge(entity);
    }

    @Transactional
    public void delete(MerchantVideoEntity entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
}
