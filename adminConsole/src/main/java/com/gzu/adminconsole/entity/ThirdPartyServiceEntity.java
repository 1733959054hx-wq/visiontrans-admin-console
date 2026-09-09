package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 第三方依赖服务台账（需求「监控核心服务及第三方接口的可用性与响应时间」）。
 *
 * <p>既覆盖外部第三方接口（翻译 / 地理编码 / 支付），也覆盖平台自身核心服务，
 * 由后台手动拨测或定时拨测写入可用性与响应耗时。
 */
@Entity
@Table(name = "third_party_service")
public class ThirdPartyServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 服务名称。 */
    @Column(name = "service_name", length = 64)
    private String name;

    /** 分类：核心服务 / 翻译引擎 / 语音识别 / 地理编码 / 支付渠道。 */
    @Column(name = "category", length = 32)
    private String category;

    /** 拨测地址。 */
    @Column(name = "endpoint", length = 256)
    private String endpoint;

    /** 拨测超时时间（毫秒）。 */
    @Column(name = "timeout_ms")
    private int timeoutMs;

    /** 当前状态：正常 / 降级 / 不可用 / 未拨测。 */
    @Column(name = "service_status", length = 16)
    private String status;

    /** 最近一次拨测耗时（毫秒）。 */
    @Column(name = "latency_ms")
    private int latencyMs;

    /** 近 24 小时可用率（%）。 */
    @Column(name = "success_rate")
    private double successRate;

    /** 最近一次拨测时间。 */
    @Column(name = "last_check", length = 32)
    private String lastCheck;

    /** 备注（降级原因等）。 */
    @Column(name = "remark", length = 256)
    private String remark;

    @Column(name = "sort_order")
    private int sortOrder;

    protected ThirdPartyServiceEntity() {
    }

    public ThirdPartyServiceEntity(String name, String category, String endpoint, int timeoutMs, String status,
                                   int latencyMs, double successRate, String lastCheck, String remark,
                                   int sortOrder) {
        this.name = name;
        this.category = category;
        this.endpoint = endpoint;
        this.timeoutMs = timeoutMs;
        this.status = status;
        this.latencyMs = latencyMs;
        this.successRate = successRate;
        this.lastCheck = lastCheck;
        this.remark = remark;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getEndpoint() { return endpoint; }
    public int getTimeoutMs() { return timeoutMs; }
    public String getStatus() { return status; }
    public int getLatencyMs() { return latencyMs; }
    public double getSuccessRate() { return successRate; }
    public String getLastCheck() { return lastCheck; }
    public String getRemark() { return remark; }
    public int getSortOrder() { return sortOrder; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setStatus(String status) { this.status = status; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public void setLastCheck(String lastCheck) { this.lastCheck = lastCheck; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
