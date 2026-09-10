package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户资金流水实体（jingchen 模块，业务表 merchant_fund_flow）。
 *
 * <p>账户管理-资金账户:充值 / 广告消耗 / 佣金入账 / 提现 / 退款 每笔一条,
 * balance_after 为流水后余额快照,可用余额由此推算。
 */
@Entity
@Table(name = "merchant_fund_flow")
public class MerchantFundFlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 流水类型:充值 / 广告消耗 / 佣金入账 / 提现 / 退款。 */
    @Column(name = "flow_type", length = 16)
    private String flowType;

    /** 收支方向:收入 / 支出。 */
    @Column(name = "direction", length = 8)
    private String direction;

    /** 发生金额(元,恒为正,方向由 direction 表达)。 */
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    /** 流水后余额(元)。 */
    @Column(name = "balance_after", precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    /** 备注 / 关联单号。 */
    @Column(name = "remark", length = 128)
    private String remark;

    /** 发生时间(yyyy-MM-dd HH:mm)。 */
    @Column(name = "created_at", length = 32)
    private String createdAt;

    protected MerchantFundFlowEntity() {
    }

    public MerchantFundFlowEntity(String flowType, String direction, BigDecimal amount,
                                  BigDecimal balanceAfter, String remark, String createdAt) {
        this.flowType = flowType;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.remark = remark;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
