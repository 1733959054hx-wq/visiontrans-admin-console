package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户经营日统计实体（jingchen 模块，业务表 merchant_daily_stat）。
 *
 * <p>经营概览的数据底座:每天一条曝光 / 点击 / 消耗 / GMV 汇总,
 * 演示数据由 {@code MerchantStatDataInitializer} 灌入,真实接入时可改为定时任务写入。
 */
@Entity
@Table(name = "merchant_daily_stat")
public class MerchantDailyStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 统计日期(yyyy-MM-dd)。 */
    @Column(name = "stat_date", length = 16)
    private String statDate;

    /** AR 广告曝光量(次)。 */
    @Column(name = "exposure")
    private Long exposure;

    /** 点击量(次)。 */
    @Column(name = "click_count")
    private Long clickCount;

    /** 广告消耗(元)。 */
    @Column(name = "consume_amount", precision = 14, scale = 2)
    private BigDecimal consume;

    /** 知识包成交 GMV(元)。 */
    @Column(name = "gmv_amount", precision = 14, scale = 2)
    private BigDecimal gmv;

    protected MerchantDailyStatEntity() {
    }

    public MerchantDailyStatEntity(String statDate, Long exposure, Long clickCount,
                                   BigDecimal consume, BigDecimal gmv) {
        this.statDate = statDate;
        this.exposure = exposure;
        this.clickCount = clickCount;
        this.consume = consume;
        this.gmv = gmv;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatDate() { return statDate; }
    public void setStatDate(String statDate) { this.statDate = statDate; }
    public Long getExposure() { return exposure; }
    public void setExposure(Long exposure) { this.exposure = exposure; }
    public Long getClickCount() { return clickCount; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }
    public BigDecimal getConsume() { return consume; }
    public void setConsume(BigDecimal consume) { this.consume = consume; }
    public BigDecimal getGmv() { return gmv; }
    public void setGmv(BigDecimal gmv) { this.gmv = gmv; }
}
