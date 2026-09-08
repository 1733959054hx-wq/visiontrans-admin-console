package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 模型版本迭代与运维时间线事件（id 越大越新）。
 */
@Entity
@Table(name = "release_event")
public class ReleaseEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "event_time", length = 32)
    private String time;

    @Column(name = "description", length = 512)
    private String desc;

    @Column(name = "tone", length = 16)
    private String tone;

    protected ReleaseEventEntity() {
    }

    public ReleaseEventEntity(String title, String time, String desc, String tone) {
        this.title = title;
        this.time = time;
        this.desc = desc;
        this.tone = tone;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
}
