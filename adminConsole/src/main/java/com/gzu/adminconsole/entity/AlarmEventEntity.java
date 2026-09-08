package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 集群告警与自愈事件（按插入顺序展示）。
 */
@Entity
@Table(name = "alarm_event")
public class AlarmEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", length = 32)
    private String time;

    @Column(name = "level", length = 8)
    private String level;

    @Column(name = "message", length = 512)
    private String message;

    @Column(name = "result", length = 32)
    private String result;

    protected AlarmEventEntity() {
    }

    public AlarmEventEntity(String time, String level, String message, String result) {
        this.time = time;
        this.level = level;
        this.message = message;
        this.result = result;
    }

    public Long getId() { return id; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
