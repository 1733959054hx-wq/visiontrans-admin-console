package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 熔断降级策略（按服务维度编排）。
 */
@Entity
@Table(name = "circuit_breaker")
public class CircuitBreakerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", length = 64)
    private String service;

    /** 策略：慢调用比例 / 异常比例 / 异常数 / 并发控制。 */
    @Column(name = "strategy", length = 32)
    private String strategy;

    /** 触发阈值文案。 */
    @Column(name = "threshold", length = 64)
    private String threshold;

    /** 状态：开启 / 半开 / 关闭 / 降级中。 */
    @Column(name = "breaker_state", length = 16)
    private String state;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "sort_order")
    private int sortOrder;

    protected CircuitBreakerEntity() {
    }

    public CircuitBreakerEntity(String service, String strategy, String threshold, String state,
                                boolean enabled, int sortOrder) {
        this.service = service;
        this.strategy = strategy;
        this.threshold = threshold;
        this.state = state;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getThreshold() { return threshold; }
    public void setThreshold(String threshold) { this.threshold = threshold; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
