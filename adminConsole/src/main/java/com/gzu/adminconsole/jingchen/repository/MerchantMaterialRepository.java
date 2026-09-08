package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantMaterialEntity;

/**
 * 商户素材数据访问层（模块自有，只操作 merchant_material 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantMaterialRepository {

    @PersistenceContext
    private EntityManager em;

    public List<MerchantMaterialEntity> findAll() {
        return em.createQuery("select m from MerchantMaterialEntity m order by m.id", MerchantMaterialEntity.class)
                .getResultList();
    }

    public MerchantMaterialEntity findById(Long id) {
        return em.find(MerchantMaterialEntity.class, id);
    }

    public long count() {
        Long count = em.createQuery("select count(m) from MerchantMaterialEntity m", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantMaterialEntity save(MerchantMaterialEntity entity) {
        return em.merge(entity);
    }

    @Transactional
    public void delete(MerchantMaterialEntity entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
}
