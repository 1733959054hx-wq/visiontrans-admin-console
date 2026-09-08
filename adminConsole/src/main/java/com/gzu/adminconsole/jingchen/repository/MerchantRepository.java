package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantEntity;

/**
 * 商户档案数据访问层（模块自有，只操作 merchant_account 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantRepository {

    @PersistenceContext
    private EntityManager em;

    /** 商户档案条数（用于判断是否需要灌入演示数据）。 */
    public long count() {
        Long count = em.createQuery("select count(m) from MerchantEntity m", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 按商户编码查找。 */
    public MerchantEntity findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        List<MerchantEntity> list = em.createQuery(
                        "select m from MerchantEntity m where m.code = :code", MerchantEntity.class)
                .setParameter("code", code)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    /** 新增商户档案。 */
    @Transactional
    public void save(MerchantEntity entity) {
        em.persist(entity);
    }

    /** 按编码重置登录口令哈希（演示账号初始化用，避免覆盖人工修改）。 */
    @Transactional
    public void updatePasswordHash(String code, String hash) {
        em.createQuery("update MerchantEntity m set m.passwordHash = :hash where m.code = :code")
                .setParameter("hash", hash)
                .setParameter("code", code)
                .executeUpdate();
    }
}
