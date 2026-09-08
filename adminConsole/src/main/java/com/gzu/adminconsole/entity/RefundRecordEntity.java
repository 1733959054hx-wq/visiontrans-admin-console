package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户退款与争议仲裁工单。
 */
@Entity
@Table(name = "refund_record")
public class RefundRecordEntity {

    @Id
    @Column(name = "record_id", length = 64)
    private String id;

    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(name = "paid", length = 32)
    private String paid;

    @Column(name = "suggest_refund", length = 32)
    private String suggestRefund;

    @Column(name = "advice", length = 64)
    private String advice;

    @Column(name = "sla", length = 64)
    private String sla;

    @Column(name = "reasons", length = 512)
    private String reasons;

    @Column(name = "ticket_status", length = 32)
    private String status;

    protected RefundRecordEntity() {
    }

    public RefundRecordEntity(String id, String orderNo, String paid, String suggestRefund, String advice,
                              String sla, String reasons, String status) {
        this.id = id;
        this.orderNo = orderNo;
        this.paid = paid;
        this.suggestRefund = suggestRefund;
        this.advice = advice;
        this.sla = sla;
        this.reasons = reasons;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPaid() { return paid; }
    public void setPaid(String paid) { this.paid = paid; }
    public String getSuggestRefund() { return suggestRefund; }
    public void setSuggestRefund(String suggestRefund) { this.suggestRefund = suggestRefund; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public String getSla() { return sla; }
    public void setSla(String sla) { this.sla = sla; }
    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
