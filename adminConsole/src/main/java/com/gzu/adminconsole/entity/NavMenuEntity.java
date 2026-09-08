package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 侧边导航菜单项。
 */
@Entity
@Table(name = "nav_menu")
public class NavMenuEntity {

    @Id
    @Column(name = "menu_id", length = 16)
    private String id;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "menu_text", length = 64)
    private String text;

    @Column(name = "sub", length = 64)
    private String sub;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "description", length = 512)
    private String desc;

    @Column(name = "sort_order")
    private int sortOrder;

    protected NavMenuEntity() {
    }

    public NavMenuEntity(String id, String icon, String text, String sub, String title, String desc,
                         int sortOrder) {
        this.id = id;
        this.icon = icon;
        this.text = text;
        this.sub = sub;
        this.title = title;
        this.desc = desc;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
