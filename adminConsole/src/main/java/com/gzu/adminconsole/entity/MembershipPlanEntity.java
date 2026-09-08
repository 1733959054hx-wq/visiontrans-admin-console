package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 会员套餐与翻译额度配置。
 */
@Entity
@Table(name = "membership_plan")
public class MembershipPlanEntity {

    @Id
    @Column(name = "plan_name", length = 64)
    private String name;

    @Column(name = "price", length = 32)
    private String price;

    @Column(name = "description", length = 128)
    private String desc;

    @Column(name = "quota", length = 128)
    private String quota;

    @Column(name = "subscribers", length = 32)
    private String subscribers;

    @Column(name = "usage_rate", length = 16)
    private String usage;

    @Column(name = "sort_order")
    private int sortOrder;

    protected MembershipPlanEntity() {
    }

    public MembershipPlanEntity(String name, String price, String desc, String quota, String subscribers,
                                String usage, int sortOrder) {
        this.name = name;
        this.price = price;
        this.desc = desc;
        this.quota = quota;
        this.subscribers = subscribers;
        this.usage = usage;
        this.sortOrder = sortOrder;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public String getQuota() { return quota; }
    public void setQuota(String quota) { this.quota = quota; }
    public String getSubscribers() { return subscribers; }
    public void setSubscribers(String subscribers) { this.subscribers = subscribers; }
    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
