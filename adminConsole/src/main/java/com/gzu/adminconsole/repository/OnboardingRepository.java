package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.MerchantOnboardingEntity;

/**
 * 商户入驻申请数据访问层。
 */
@Repository
@Transactional(readOnly = true)
public class OnboardingRepository {

    @PersistenceContext
    private EntityManager em;

    /** 全部入驻申请（按主键倒序，最新在前）。 */
    public List<MerchantOnboardingEntity> findAll() {
        return em.createQuery("select m from MerchantOnboardingEntity m order by m.id desc",
                MerchantOnboardingEntity.class).getResultList();
    }

    public MerchantOnboardingEntity findById(Long id) {
        return em.find(MerchantOnboardingEntity.class, id);
    }

    @Transactional
    public void update(MerchantOnboardingEntity entity) {
        em.merge(entity);
    }

    @Transactional
    public void saveAll(List<MerchantOnboardingEntity> rows) {
        for (MerchantOnboardingEntity row : rows) {
            em.persist(row);
        }
    }

    public boolean isEmpty() {
        Long count = em.createQuery("select count(m) from MerchantOnboardingEntity m", Long.class).getSingleResult();
        return count == null || count == 0L;
    }
}
