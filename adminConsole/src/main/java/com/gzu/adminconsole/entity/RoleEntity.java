package com.gzu.adminconsole.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * RBAC 角色域（级联持有权限组与细粒度权限）。
 */
@Entity
@Table(name = "security_role")
public class RoleEntity {

    @Id
    @Column(name = "role_code", length = 32)
    private String code;

    @Column(name = "role_name", length = 64)
    private String name;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "members")
    private int members;

    @Column(name = "sort_order")
    private int sortOrder;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "role_code")
    @OrderBy("sortOrder ASC")
    private List<PermGroupEntity> groups = new ArrayList<>();

    protected RoleEntity() {
    }

    public RoleEntity(String code, String name, String icon, int members, int sortOrder) {
        this.code = code;
        this.name = name;
        this.icon = icon;
        this.members = members;
        this.sortOrder = sortOrder;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public int getMembers() { return members; }
    public void setMembers(int members) { this.members = members; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<PermGroupEntity> getGroups() { return groups; }
    public void setGroups(List<PermGroupEntity> groups) { this.groups = groups; }
}
