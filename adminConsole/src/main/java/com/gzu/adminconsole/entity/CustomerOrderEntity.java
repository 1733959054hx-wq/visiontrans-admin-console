package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * C 端用户订单（会员购买 / 课程付费 / 数字内容购买）。
 */
@Entity
@Table(name = "customer_order")
public class CustomerOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", length = 32)
    private String orderNo;

    @Column(name = "customer", length = 64)
    private String customer;

    /** 订单类型：会员 / 课程 / 数字内容。 */
    @Column(name = "order_type", length = 32)
    private String type;

    @Column(name = "amount", length = 32)
    private String amount;

    /** 状态：待支付 / 已支付 / 支付异常 / 退款中 / 已退款 / 已关闭。 */
    @Column(name = "order_status", length = 16)
    private String status;

    @Column(name = "created", length = 32)
    private String created;

    @Column(name = "note", length = 128)
    private String note;

    protected CustomerOrderEntity() {
    }

    public CustomerOrderEntity(String orderNo, String customer, String type, String amount,
                               String status, String created, String note) {
        this.orderNo = orderNo;
        this.customer = customer;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.created = created;
        this.note = note;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
