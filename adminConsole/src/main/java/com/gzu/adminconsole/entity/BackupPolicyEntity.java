package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 备份策略（备份对象 / 周期 / 保留期 / 存储位置）。
 */
@Entity
@Table(name = "backup_policy")
public class BackupPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target", length = 64)
    private String target;

    /** 备份周期：每日 / 每周 / 每次发布。 */
    @Column(name = "cycle", length = 16)
    private String cycle;

    @Column(name = "retention", length = 32)
    private String retention;

    @Column(name = "storage", length = 64)
    private String storage;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "sort_order")
    private int sortOrder;

    protected BackupPolicyEntity() {
    }

    public BackupPolicyEntity(String target, String cycle, String retention, String storage,
                              boolean enabled, int sortOrder) {
        this.target = target;
        this.cycle = cycle;
        this.retention = retention;
        this.storage = storage;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }
    public String getRetention() { return retention; }
    public void setRetention(String retention) { this.retention = retention; }
    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
