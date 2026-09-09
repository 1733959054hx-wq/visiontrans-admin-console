package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户周期结算记录（对账周期 T+1）。
 */
@Entity
@Table(name = "settlement_record")
public class SettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant", length = 64)
    private String merchant;

    /** 结算周期（yyyy-MM）。 */
    @Column(name = "period", length = 16)
    private String period;

    /** 周期内订单数。 */
    @Column(name = "orders")
    private int orders;

    @Column(name = "amount", length = 32)
    private String amount;

    @Column(name = "commission", length = 32)
    private String commission;

    /** 状态：待对账 / 已对账 / 已结算。 */
    @Column(name = "settle_status", length = 16)
    private String status;

    protected SettlementEntity() {
    }

    public SettlementEntity(String merchant, String period, int orders, String amount,
                            String commission, String status) {
        this.merchant = merchant;
        this.period = period;
        this.orders = orders;
        this.amount = amount;
        this.commission = commission;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public int getOrders() { return orders; }
    public void setOrders(int orders) { this.orders = orders; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCommission() { return commission; }
    public void setCommission(String commission) { this.commission = commission; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
