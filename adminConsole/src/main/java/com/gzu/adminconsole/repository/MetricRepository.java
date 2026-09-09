package com.gzu.adminconsole.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.MetricSampleEntity;

/**
 * 时序指标采样数据访问层。
 *
 * <p>所有查询都支持 yyyy-MM-dd 日期范围过滤；日期一律参数绑定，无注入风险。
 */
@Repository
@Transactional(readOnly = true)
public class MetricRepository {

    /** 指标键常量。 */
    public static final class MetricKey {

        /** 集群 QPS（按小时）。 */
        public static final String CLUSTER_QPS_HOURLY = "cluster.qps.hourly";
        /** 端到端延迟：管线分段耗时（按天一份口径）。 */
        public static final String LATENCY_SEGMENT = "cluster.latency.segment";
        /** 端到端延迟：波形采样点（按天 × 层 × 点）。 */
        public static final String LATENCY_WAVE = "cluster.latency.wave";
        /** 安全：越权访问拦截事件。 */
        public static final String SECURITY_BLOCK = "security.block";
        /** 广告：调度请求数（按小时）。 */
        public static final String ADS_REQUEST_HOURLY = "ads.request.hourly";
        /** 模型：自动回滚事件。 */
        public static final String MODEL_ROLLBACK = "model.rollback";
        /** 集群：采样可用率（%，每天一条）。 */
        public static final String CLUSTER_AVAILABILITY = "cluster.sample.availability";
        /** 集群：数据时延（秒，每天一条）。 */
        public static final String CLUSTER_DATA_DELAY_SECONDS = "cluster.data.delay.seconds";
        /** 模型：平均热更耗时（秒，每天一条）。 */
        public static final String MODEL_HOTFIX_SECONDS = "model.hotfix.duration";
        /** 广告：平均决策耗时（毫秒，每天一条）。 */
        public static final String ADS_DECISION_MS = "ads.decision.ms";
        /** 广告：填充率（%，每天一条）。 */
        public static final String ADS_FILL_RATE = "ads.fill.rate";
        /** 广告：曝光量（按小时）。 */
        public static final String ADS_IMPRESSION_HOURLY = "ads.impression.hourly";
        /** 广告：点击量（按小时）。 */
        public static final String ADS_CLICK_HOURLY = "ads.click.hourly";
        /** 广告：转化量（按小时）。 */
        public static final String ADS_CONVERSION_HOURLY = "ads.conversion.hourly";

        private MetricKey() {
        }
    }

    @PersistenceContext
    private EntityManager em;

    /** 采样总条数（用于判断是否需要灌入演示数据）。 */
    public long count() {
        Long count = em.createQuery("select count(m) from MetricSampleEntity m", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 单项指标的采样条数（用于老库升级时判断该指标是否需要补灌）。 */
    public long countByKey(String metricKey) {
        Long count = em.createQuery("select count(m) from MetricSampleEntity m where m.metricKey = :key",
                        Long.class)
                .setParameter("key", metricKey)
                .getSingleResult();
        return count == null ? 0L : count;
    }

    /**
     * 清理某指标在「同日 + 同时刻」下的重复采样（每组保留最早一条），返回删除行数。
     *
     * <p>用于老库升级补灌后的幂等修复：无重复时影响 0 行，可安全重复调用。
     */
    @Transactional
    public int dedupHourlySamples(String metricKey) {
        return em.createNativeQuery(
                        "delete m1 from metric_sample m1"
                        + " join metric_sample m2 on m1.metric_key = m2.metric_key"
                        + " and m1.sample_date = m2.sample_date and m1.bucket_label = m2.bucket_label"
                        + " and m1.id > m2.id where m1.metric_key = :key")
                .setParameter("key", metricKey)
                .executeUpdate();
    }

    /** 批量写入采样数据。 */
    @Transactional
    public void saveAll(List<MetricSampleEntity> rows) {
        for (MetricSampleEntity row : rows) {
            em.persist(row);
        }
    }

    /**
     * 取某项指标在范围内**最新一天**的采样（用于"当天口径"的图表，如延迟波形）。
     * 范围内无数据时返回空列表。
     */
    public List<MetricSampleEntity> latestDay(String metricKey, String start, String end) {
        TypedQuery<String> maxQuery = em.createQuery(
                "select max(m.sampleDate) from MetricSampleEntity m where m.metricKey = :key"
                        + rangeClause(start, end), String.class);
        maxQuery.setParameter("key", metricKey);
        bindRange(maxQuery, start, end);
        String latest = maxQuery.getResultList().stream().findFirst().orElse(null);
        if (latest == null) {
            return List.of();
        }
        return em.createQuery(
                        "select m from MetricSampleEntity m where m.metricKey = :key and m.sampleDate = :date"
                                + " order by m.seq, m.id", MetricSampleEntity.class)
                .setParameter("key", metricKey)
                .setParameter("date", latest)
                .getResultList();
    }

    /** 按 bucket 求均值（key = 分组标签）。 */
    public Map<String, Double> avgByBucket(String metricKey, String start, String end) {
        Query query = em.createQuery(
                "select m.bucket, avg(m.value) from MetricSampleEntity m where m.metricKey = :key"
                        + rangeClause(start, end) + " group by m.bucket order by m.bucket", Object[].class);
        query.setParameter("key", metricKey);
        bindRange(query, start, end);
        return toMap(query.getResultList());
    }

    /** 按采样日期求和（key = yyyy-MM-dd，按日期升序）。 */
    public Map<String, Double> sumByDate(String metricKey, String start, String end) {
        Query query = em.createQuery(
                "select m.sampleDate, sum(m.value) from MetricSampleEntity m where m.metricKey = :key"
                        + rangeClause(start, end) + " group by m.sampleDate order by m.sampleDate",
                Object[].class);
        query.setParameter("key", metricKey);
        bindRange(query, start, end);
        return toMap(query.getResultList());
    }

    /** 范围内均值（单值指标如可用率、耗时等）。 */
    public double avg(String metricKey, String start, String end) {
        Query query = em.createQuery(
                "select coalesce(avg(m.value), 0) from MetricSampleEntity m where m.metricKey = :key"
                        + rangeClause(start, end), Double.class);
        query.setParameter("key", metricKey);
        bindRange(query, start, end);
        Double value = (Double) query.getSingleResult();
        return value == null ? 0.0 : value;
    }

    /** 范围内求和（事件计数、请求数等）。 */
    public double sum(String metricKey, String start, String end) {
        Query query = em.createQuery(
                "select coalesce(sum(m.value), 0) from MetricSampleEntity m where m.metricKey = :key"
                        + rangeClause(start, end), Double.class);
        query.setParameter("key", metricKey);
        bindRange(query, start, end);
        Double total = (Double) query.getSingleResult();
        return total == null ? 0.0 : total;
    }

    private static Map<String, Double> toMap(List<Object[]> rows) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Object[] row : rows) {
            out.put(String.valueOf(row[0]), ((Number) row[1]).doubleValue());
        }
        return out;
    }

    /** 日期范围条件骨架（参数后面单独绑定）。 */
    private static String rangeClause(String start, String end) {
        StringBuilder clause = new StringBuilder();
        if (start != null && !start.isBlank()) {
            clause.append(" and m.sampleDate >= :start");
        }
        if (end != null && !end.isBlank()) {
            clause.append(" and m.sampleDate <= :end");
        }
        return clause.toString();
    }

    private static void bindRange(Query query, String start, String end) {
        if (start != null && !start.isBlank()) {
            query.setParameter("start", start);
        }
        if (end != null && !end.isBlank()) {
            query.setParameter("end", end);
        }
    }
}
