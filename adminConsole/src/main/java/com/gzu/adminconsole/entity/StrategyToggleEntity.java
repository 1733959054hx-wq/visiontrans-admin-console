package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 功能开关（按 groupName 区分所属模块：models / ads）。
 */
@Entity
@Table(name = "strategy_toggle")
public class StrategyToggleEntity {

    @Id
    @Column(name = "toggle_name", length = 64)
    private String name;

    @Column(name = "group_name", length = 32)
    private String groupName;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "sort_order")
    private int sortOrder;

    protected StrategyToggleEntity() {
    }

    public StrategyToggleEntity(String name, String groupName, boolean enabled, int sortOrder) {
        this.name = name;
        this.groupName = groupName;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
