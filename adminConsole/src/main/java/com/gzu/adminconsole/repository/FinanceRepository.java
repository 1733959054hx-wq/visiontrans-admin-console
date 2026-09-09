package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.CustomerOrderEntity;
import com.gzu.adminconsole.entity.InvoiceEntity;
import com.gzu.adminconsole.entity.SettlementEntity;
import com.gzu.adminconsole.model.CustomerOrder;
import com.gzu.adminconsole.model.InvoiceApplication;
import com.gzu.adminconsole.model.SettlementRecord;

/**
 * 财务订单、商户结算与发票审核数据访问层（JPA 实现）。
 */
@Repository
@Transactional(readOnly = true)
public class FinanceRepository {

    @PersistenceContext
    private EntityManager em;

    /* ------------------------------ C 端订单 ------------------------------ */

    /** 全部 C 端订单（按下单顺序）。 */
    public List<CustomerOrder> findOrders() {
        return em.createQuery("select o from CustomerOrderEntity o order by o.id", CustomerOrderEntity.class)
                .getResultList().stream().map(this::toOrderModel).toList();
    }

    /** 按主键查找订单。 */
    public CustomerOrder findOrder(Long id) {
        CustomerOrderEntity entity = em.find(CustomerOrderEntity.class, id);
        return entity == null ? null : toOrderModel(entity);
    }

    /** 更新订单（整行覆盖）。 */
    @Transactional
    public void updateOrder(CustomerOrder order) {
        CustomerOrderEntity entity = em.find(CustomerOrderEntity.class, order.id());
        if (entity == null) {
            return;
        }
        entity.setOrderNo(order.orderNo());
        entity.setCustomer(order.customer());
        entity.setType(order.type());
        entity.setAmount(order.amount());
        entity.setStatus(order.status());
        entity.setCreated(order.created());
        entity.setNote(order.note());
        em.merge(entity);
    }

    /** 订单总数。 */
    public long countOrders() {
        Long count = em.createQuery("select count(o) from CustomerOrderEntity o", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 订单表是否为空。 */
    public boolean ordersEmpty() {
        return countOrders() == 0L;
    }

    @Transactional
    public void saveOrders(List<CustomerOrder> orders) {
        orders.forEach(o -> em.persist(new CustomerOrderEntity(o.orderNo(), o.customer(), o.type(), o.amount(),
                o.status(), o.created(), o.note())));
    }

    /* ------------------------------ 商户结算 ------------------------------ */

    /** 全部结算记录。 */
    public List<SettlementRecord> findSettlements() {
        return em.createQuery("select s from SettlementEntity s order by s.id", SettlementEntity.class)
                .getResultList().stream().map(this::toSettlementModel).toList();
    }

    /** 按主键查找结算记录。 */
    public SettlementRecord findSettlement(Long id) {
        SettlementEntity entity = em.find(SettlementEntity.class, id);
        return entity == null ? null : toSettlementModel(entity);
    }

    /** 更新结算记录。 */
    @Transactional
    public void updateSettlement(SettlementRecord record) {
        SettlementEntity entity = em.find(SettlementEntity.class, record.id());
        if (entity == null) {
            return;
        }
        entity.setMerchant(record.merchant());
        entity.setPeriod(record.period());
        entity.setOrders(record.orders());
        entity.setAmount(record.amount());
        entity.setCommission(record.commission());
        entity.setStatus(record.status());
        em.merge(entity);
    }

    /** 结算记录总数。 */
    public long countSettlements() {
        Long count = em.createQuery("select count(s) from SettlementEntity s", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 结算表是否为空。 */
    public boolean settlementsEmpty() {
        return countSettlements() == 0L;
    }

    @Transactional
    public void saveSettlements(List<SettlementRecord> records) {
        records.forEach(s -> em.persist(new SettlementEntity(s.merchant(), s.period(), s.orders(), s.amount(),
                s.commission(), s.status())));
    }

    /* ------------------------------ 发票申请 ------------------------------ */

    /** 全部发票申请。 */
    public List<InvoiceApplication> findInvoices() {
        return em.createQuery("select i from InvoiceEntity i order by i.id", InvoiceEntity.class)
                .getResultList().stream().map(this::toInvoiceModel).toList();
    }

    /** 按主键查找发票申请。 */
    public InvoiceApplication findInvoice(Long id) {
        InvoiceEntity entity = em.find(InvoiceEntity.class, id);
        return entity == null ? null : toInvoiceModel(entity);
    }

    /** 更新发票申请。 */
    @Transactional
    public void updateInvoice(InvoiceApplication invoice) {
        InvoiceEntity entity = em.find(InvoiceEntity.class, invoice.id());
        if (entity == null) {
            return;
        }
        entity.setApplyNo(invoice.applyNo());
        entity.setApplicant(invoice.applicant());
        entity.setTitle(invoice.title());
        entity.setTaxNo(invoice.taxNo());
        entity.setAmount(invoice.amount());
        entity.setStatus(invoice.status());
        entity.setApplied(invoice.applied());
        em.merge(entity);
    }

    /** 发票申请总数。 */
    public long countInvoices() {
        Long count = em.createQuery("select count(i) from InvoiceEntity i", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 发票表是否为空。 */
    public boolean invoicesEmpty() {
        return countInvoices() == 0L;
    }

    @Transactional
    public void saveInvoices(List<InvoiceApplication> invoices) {
        invoices.forEach(i -> em.persist(new InvoiceEntity(i.applyNo(), i.applicant(), i.title(), i.taxNo(),
                i.amount(), i.status(), i.applied())));
    }

    /* ------------------------------ 模型转换 ------------------------------ */

    private CustomerOrder toOrderModel(CustomerOrderEntity o) {
        return new CustomerOrder(o.getId(), o.getOrderNo(), o.getCustomer(), o.getType(), o.getAmount(),
                o.getStatus(), o.getCreated(), o.getNote());
    }

    private SettlementRecord toSettlementModel(SettlementEntity s) {
        return new SettlementRecord(s.getId(), s.getMerchant(), s.getPeriod(), s.getOrders(), s.getAmount(),
                s.getCommission(), s.getStatus());
    }

    private InvoiceApplication toInvoiceModel(InvoiceEntity i) {
        return new InvoiceApplication(i.getId(), i.getApplyNo(), i.getApplicant(), i.getTitle(), i.getTaxNo(),
                i.getAmount(), i.getStatus(), i.getApplied());
    }
}
