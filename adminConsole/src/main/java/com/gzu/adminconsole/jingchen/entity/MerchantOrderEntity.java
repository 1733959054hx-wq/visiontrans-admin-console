package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户订单实体（jingchen 模块，业务表 merchant_order）。
 *
 * <p>订单与结算模块的数据底座:记录渠道成交订单与佣金,支持「待结算 → 已结算」流转,
 * 退款中为终态演示。演示数据由 {@code MerchantOrderDataInitializer} 灌入。
 */
@Entity
@Table(name = "merchant_order")
public class MerchantOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号,如 ORD-20260903-001。 */
    @Column(name = "order_no", length = 40)
    private String orderNo;

    /** 成交商品(知识包 / 课程 / 视频内容)。 */
    @Column(name = "goods_name", length = 128)
    private String goodsName;

    /** 成交渠道(抖音内容号 / 小红书达人 / 旅行社直客 / 自有门店)。 */
    @Column(name = "channel", length = 32)
    private String channel;

    /** 成交金额(元)。 */
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    /** 分销佣金(元,= 成交金额 × 佣金比例)。 */
    @Column(name = "commission", precision = 12, scale = 2)
    private BigDecimal commission;

    /** 结算状态:待结算 / 已结算 / 退款中。 */
    @Column(name = "settle_status", length = 16)
    private String status;

    /** 下单时间(yyyy-MM-dd HH:mm)。 */
    @Column(name = "created_at", length = 32)
    private String createdAt;

    protected MerchantOrderEntity() {
    }

    public MerchantOrderEntity(String orderNo, String goodsName, String channel, BigDecimal amount,
                               BigDecimal commission, String status, String createdAt) {
        this.orderNo = orderNo;
        this.goodsName = goodsName;
        this.channel = channel;
        this.amount = amount;
        this.commission = commission;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
