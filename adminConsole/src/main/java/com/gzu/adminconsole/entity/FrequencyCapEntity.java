package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 单用户跨广告位联合频控项。
 */
@Entity
@Table(name = "frequency_cap")
public class FrequencyCapEntity {

    @Id
    @Column(name = "cap_name", length = 64)
    private String name;

    @Column(name = "display", length = 64)
    private String display;

    @Column(name = "max_value")
    private int maxValue;

    @Column(name = "cap_value")
    private int value;

    @Column(name = "sort_order")
    private int sortOrder;

    protected FrequencyCapEntity() {
    }

    public FrequencyCapEntity(String name, String display, int maxValue, int value, int sortOrder) {
        this.name = name;
        this.display = display;
        this.maxValue = maxValue;
        this.value = value;
        this.sortOrder = sortOrder;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplay() { return display; }
    public void setDisplay(String display) { this.display = display; }
    public int getMaxValue() { return maxValue; }
    public void setMaxValue(int maxValue) { this.maxValue = maxValue; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
