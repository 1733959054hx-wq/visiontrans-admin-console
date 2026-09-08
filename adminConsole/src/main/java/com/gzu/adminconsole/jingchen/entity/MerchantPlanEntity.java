package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户投放计划实体（jingchen 模块，业务表 ad_plan）。
 *
 * <p>模块自有业务表，不写主工程的导航 / 审核体系；字段按后台管理表习惯显式命名，
 * 便于将来接入 flyway 迁移脚本。演示环境单商户共享该表，多商户可扩展 merchant_code 字段。
 */
@Entity
@Table(name = "ad_plan")
public class MerchantPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务编号,如 PLAN-2026-1031。 */
    @Column(name = "plan_no", length = 32)
    private String planNo;

    /** 计划名称。 */
    @Column(name = "plan_name", nullable = false, length = 128)
    private String name;

    /** 广告形式:AR 街景锁定 / Banner 信息流 / 开屏广告。 */
    @Column(name = "ad_form", length = 32)
    private String adForm;

    /** 定向场景,如 机场口岸 · 中→日。 */
    @Column(name = "scene", length = 64)
    private String scene;

    /** 日预算(元)。 */
    @Column(name = "budget", precision = 14, scale = 2)
    private BigDecimal budget;

    /** 已消耗(元)。 */
    @Column(name = "used_amount", precision = 14, scale = 2)
    private BigDecimal used;

    /** 投放状态:投放中 / 预算预警 / 已暂停 / 待审核。 */
    @Column(name = "plan_status", length = 16)
    private String status;

    /** 项目负责人。 */
    @Column(name = "owner_name", length = 32)
    private String owner;

    /** 点击率(%),演示数据可为空。 */
    @Column(name = "ctr", precision = 5, scale = 2)
    private BigDecimal ctr;

    /** 暂停标记(与 status=已暂停 同步维护,便于列表快速过滤)。 */
    @Column(name = "paused")
    private Boolean paused = false;

    protected MerchantPlanEntity() {
    }

    public MerchantPlanEntity(String planNo, String name, String adForm, String scene,
                              BigDecimal budget, BigDecimal used, String status,
                              String owner, BigDecimal ctr, Boolean paused) {
        this.planNo = planNo;
        this.name = name;
        this.adForm = adForm;
        this.scene = scene;
        this.budget = budget;
        this.used = used;
        this.status = status;
        this.owner = owner;
        this.ctr = ctr;
        this.paused = paused;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanNo() { return planNo; }
    public void setPlanNo(String planNo) { this.planNo = planNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdForm() { return adForm; }
    public void setAdForm(String adForm) { this.adForm = adForm; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getUsed() { return used; }
    public void setUsed(BigDecimal used) { this.used = used; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public BigDecimal getCtr() { return ctr; }
    public void setCtr(BigDecimal ctr) { this.ctr = ctr; }
    public Boolean getPaused() { return paused; }
    public void setPaused(Boolean paused) { this.paused = paused; }
}
