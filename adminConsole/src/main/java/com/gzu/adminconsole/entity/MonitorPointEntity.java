package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 全球登录监控点位。
 */
@Entity
@Table(name = "monitor_point")
public class MonitorPointEntity {

    @Id
    @Column(name = "point_name", length = 64)
    private String name;

    @Column(name = "lon")
    private double lon;

    @Column(name = "lat")
    private double lat;

    @Column(name = "sessions")
    private int sessions;

    @Column(name = "level", length = 16)
    private String level;

    @Column(name = "sort_order")
    private int sortOrder;

    protected MonitorPointEntity() {
    }

    public MonitorPointEntity(String name, double lon, double lat, int sessions, String level, int sortOrder) {
        this.name = name;
        this.lon = lon;
        this.lat = lat;
        this.sessions = sessions;
        this.level = level;
        this.sortOrder = sortOrder;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public int getSessions() { return sessions; }
    public void setSessions(int sessions) { this.sessions = sessions; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
