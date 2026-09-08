package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 通用时序指标采样表。
 *
 * <p>用于承载原本在代码里写死的监控类指标（KPI 计数、延迟波形、QPS 曲线、广告调度请求等），
 * 使所有展示数据都来自数据库统计，并天然支持按日期范围过滤。
 *
 * <p>metricKey 取值见 {@link com.gzu.adminconsole.repository.MetricRepository.MetricKey}。
 */
@Entity
@Table(name = "metric_sample",
        indexes = {@Index(name = "idx_metric_key_date", columnList = "metric_key, sample_date")})
public class MetricSampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指标键，如 cluster.qps.hourly。 */
    @Column(name = "metric_key", length = 64, nullable = false)
    private String metricKey;

    /** 采样日期 yyyy-MM-dd（日期范围过滤即作用于此列）。 */
    @Column(name = "sample_date", length = 10, nullable = false)
    private String sampleDate;

    /** 分组标签：管线层名 / 小时 / 天等。 */
    @Column(name = "bucket_label", length = 32)
    private String bucket;

    /** 序号：横轴位置或排序用。 */
    @Column(name = "seq_no")
    private int seq;

    /** 采样值：耗时 ms / QPS / 请求数 / 事件计数（事件类为 1）。 */
    @Column(name = "sample_value")
    private double value;

    /** 可选展示色（渐变起色）。 */
    @Column(name = "c1", length = 16)
    private String c1;

    /** 可选展示色（渐变止色）。 */
    @Column(name = "c2", length = 16)
    private String c2;

    protected MetricSampleEntity() {
    }

    public MetricSampleEntity(String metricKey, String sampleDate, String bucket, int seq, double value,
                              String c1, String c2) {
        this.metricKey = metricKey;
        this.sampleDate = sampleDate;
        this.bucket = bucket;
        this.seq = seq;
        this.value = value;
        this.c1 = c1;
        this.c2 = c2;
    }

    public Long getId() { return id; }
    public String getMetricKey() { return metricKey; }
    public String getSampleDate() { return sampleDate; }
    public String getBucket() { return bucket; }
    public int getSeq() { return seq; }
    public double getValue() { return value; }
    public String getC1() { return c1; }
    public String getC2() { return c2; }
}
