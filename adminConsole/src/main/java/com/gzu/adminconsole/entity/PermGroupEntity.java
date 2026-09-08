package com.gzu.adminconsole.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * 权限组（级联持有细粒度权限项）。
 */
@Entity
@Table(name = "perm_group")
public class PermGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", length = 64)
    private String name;

    @Column(name = "members")
    private int members;

    @Column(name = "sort_order")
    private int sortOrder;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "group_id")
    @OrderBy("sortOrder ASC")
    private List<PermissionEntity> permissions = new ArrayList<>();

    protected PermGroupEntity() {
    }

    public PermGroupEntity(String name, int members, int sortOrder) {
        this.name = name;
        this.members = members;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMembers() { return members; }
    public void setMembers(int members) { this.members = members; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<PermissionEntity> getPermissions() { return permissions; }
    public void setPermissions(List<PermissionEntity> permissions) { this.permissions = permissions; }
}
