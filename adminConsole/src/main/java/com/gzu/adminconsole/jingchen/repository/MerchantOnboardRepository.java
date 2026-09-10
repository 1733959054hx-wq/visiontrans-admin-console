package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.MerchantOnboardingEntity;

/**
 * 商户端入驻申请数据访问（jingchen 模块,查询本模块商户名关联的申请单,
 * 复用主工程 merchant_onboarding 表与实体,不重复建表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantOnboardRepository {

    @PersistenceContext
    private EntityManager em;

    /** 按商户名查本商户的申请单(最新在前)。 */
    public List<com.gzu.adminconsole.entity.MerchantOnboardingEntity> findByName(String merchantName) {
        return em.createQuery("select o from MerchantOnboardingEntity o where o.merchantName = :n order by o.id desc",
                        MerchantOnboardingEntity.class)
                .setParameter("n", merchantName)
                .getResultList();
    }

    @Transactional
    public MerchantOnboardingEntity save(MerchantOnboardingEntity entity) {
        return em.merge(entity);
    }
}
