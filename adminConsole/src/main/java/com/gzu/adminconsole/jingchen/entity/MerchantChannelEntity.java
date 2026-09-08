package com.gzu.adminconsole.jingchen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户推广渠道实体（jingchen 模块，业务表 merchant_channel）。
 *
 * <p>内容分销-推广管理:每个渠道一条记录,含佣金比例与点击 / 成交表现;
 * 二维码与短链由前端按渠道码即时生成,不入库。演示数据由 {@code MerchantPromoDataInitializer} 灌入。
 */
@Entity
@Table(name = "merchant_channel")
public class MerchantChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 渠道名称,如 抖音内容号。 */
    @Column(name = "channel_name", length = 64)
    private String channelName;

    /** 渠道码(用于生成短链与二维码),如 DOUYIN-20875。 */
    @Column(name = "channel_code", length = 40)
    private String channelCode;

    /** 佣金比例(%,5-50)。 */
    @Column(name = "ratio_percent")
    private Integer ratioPercent;

    /** 累计点击。 */
    @Column(name = "click_count")
    private Long clickCount;

    /** 累计成交(笔)。 */
    @Column(name = "deal_count")
    private Long dealCount;

    /** 预计待结算佣金(元)。 */
    @Column(name = "commission_amount", precision = 12, scale = 2)
    private java.math.BigDecimal commissionAmount;

    protected MerchantChannelEntity() {
    }

    public MerchantChannelEntity(String channelName, String channelCode, Integer ratioPercent,
                                 Long clickCount, Long dealCount, java.math.BigDecimal commissionAmount) {
        this.channelName = channelName;
        this.channelCode = channelCode;
        this.ratioPercent = ratioPercent;
        this.clickCount = clickCount;
        this.dealCount = dealCount;
        this.commissionAmount = commissionAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public Integer getRatioPercent() { return ratioPercent; }
    public void setRatioPercent(Integer ratioPercent) { this.ratioPercent = ratioPercent; }
    public Long getClickCount() { return clickCount; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }
    public Long getDealCount() { return dealCount; }
    public void setDealCount(Long dealCount) { this.dealCount = dealCount; }
    public java.math.BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(java.math.BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
}
