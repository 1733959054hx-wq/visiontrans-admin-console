package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 广告位库存（甘特图的逐日格子由 ratio 确定性推导，不落库）。
 */
@Entity
@Table(name = "ad_slot")
public class AdSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_name", length = 128)
    private String name;

    @Column(name = "slot_status", length = 32)
    private String status;

    @Column(name = "color", length = 16)
    private String color;

    @Column(name = "remain", length = 16)
    private String remain;

    @Column(name = "ratio")
    private double ratio;

    @Column(name = "sort_order")
    private int sortOrder;

    /** 上下线状态：null 视为上线（兼容老数据）。 */
    @Column(name = "online")
    private Boolean online = true;

    protected AdSlotEntity() {
    }

    public AdSlotEntity(String name, String status, String color, String remain, double ratio, int sortOrder) {
        this.name = name;
        this.status = status;
        this.color = color;
        this.remain = remain;
        this.ratio = ratio;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getRemain() { return remain; }
    public void setRemain(String remain) { this.remain = remain; }
    public double getRatio() { return ratio; }
    public void setRatio(double ratio) { this.ratio = ratio; }
    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
