package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 细粒度权限项（三态：GRANTED / PARTIAL / NONE）。
 */
@Entity
@Table(name = "permission_item")
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_name", length = 128)
    private String name;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "sort_order")
    private int sortOrder;

    protected PermissionEntity() {
    }

    public PermissionEntity(String name, String state, int sortOrder) {
        this.name = name;
        this.state = state;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
