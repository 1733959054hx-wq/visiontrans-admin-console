package com.gzu.adminconsole.jingchen.repository;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantFundFlowEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantWithdrawEntity;

/**
 * 商户资金数据访问层（模块自有,操作 merchant_fund_flow 与 merchant_withdraw 两表,
 * 同 AuthRepository 处理双实体 precedent）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantFundsRepository {

    @PersistenceContext
    private EntityManager em;

    /** 流水按时间倒序(最新在前)。 */
    public List<MerchantFundFlowEntity> findFlows() {
        return em.createQuery("select f from MerchantFundFlowEntity f order by f.id desc",
                        MerchantFundFlowEntity.class)
                .getResultList();
    }

    /** 提现记录按时间倒序。 */
    public List<MerchantWithdrawEntity> findWithdraws() {
        return em.createQuery("select w from MerchantWithdrawEntity w order by w.id desc",
                        MerchantWithdrawEntity.class)
                .getResultList();
    }

    public long countFlows() {
        Long count = em.createQuery("select count(f) from MerchantFundFlowEntity f", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    public long countWithdraws() {
        Long count = em.createQuery("select count(w) from MerchantWithdrawEntity w", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantFundFlowEntity saveFlow(MerchantFundFlowEntity flow) {
        return em.merge(flow);
    }

    @Transactional
    public MerchantWithdrawEntity saveWithdraw(MerchantWithdrawEntity withdraw) {
        return em.merge(withdraw);
    }
}
