package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.AdSlotEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantProfileExtEntity;

/**
 * 商户资料与广告位数据访问（jingchen 模块）。
 *
 * <p>① 复用主工程 merchant_account 实体维护商户资料;② 只读查询主工程
 * ad_slot 广告位库存(投放设置的「广告位选择」数据源);③ 模块自有资料扩展表。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantProfileRepository {

    @PersistenceContext
    private EntityManager em;

    /** 按登录账号查商户档案。 */
    public MerchantEntity findAccount(String code) {
        List<MerchantEntity> list = em.createQuery(
                        "select m from MerchantEntity m where m.code = :code",
                        MerchantEntity.class)
                .setParameter("code", code)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public void saveAccount(MerchantEntity entity) {
        em.merge(entity);
    }

    /** 可上线的广告位列表(按排序位)。 */
    public List<AdSlotEntity> findOnlineSlots() {
        return em.createQuery(
                        "select s from AdSlotEntity s where s.online = true order by s.sortOrder",
                        AdSlotEntity.class)
                .getResultList();
    }

    /** 按商户编码查资料扩展。 */
    public MerchantProfileExtEntity findProfileExt(String merchantCode) {
        List<MerchantProfileExtEntity> list = em.createQuery(
                        "select p from MerchantProfileExtEntity p where p.merchantCode = :c",
                        MerchantProfileExtEntity.class)
                .setParameter("c", merchantCode)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public void saveProfileExt(MerchantProfileExtEntity ext) {
        em.merge(ext);
    }
}
