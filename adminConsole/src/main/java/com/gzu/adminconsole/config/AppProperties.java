package com.gzu.adminconsole.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全局业务配置项，对应 application.properties 中 {@code admin-console.*} 前缀。
 *
 * <p>所有字段均带默认值，即使配置文件缺失对应项也能正常启动，保证可移植性。
 */
@ConfigurationProperties(prefix = "admin-console")
public class AppProperties {

    /** REST 接口相关配置。 */
    private Api api = new Api();
    /** 平台品牌与界面展示信息。 */
    private Platform platform = new Platform();
    /** 跨域配置。 */
    private Cors cors = new Cors();
    /** 集群运维阈值。 */
    private Cluster cluster = new Cluster();
    /** 安全合规配置。 */
    private Security security = new Security();
    /** 演示数据开关。 */
    private Data data = new Data();

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    /** REST 接口配置。 */
    public static class Api {

        /** 是否开放 REST 接口（关闭后仅启动容器，便于排查）。 */
        private boolean enabled = true;
        /** 接口统一前缀。 */
        private String basePath = "/api";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }

    /** 平台品牌与界面展示信息。 */
    public static class Platform {

        private String name = "视界译 VisionTrans";
        private String subtitle = "VR 实时解析与翻译系统";
        private String consoleLabel = "平台管理后台 · Admin Console";
        private String groupName = "平台治理";
        private String clusterStatus = "集群正常";
        private String clusterLatency = "端到端 186ms";
        private String dateRange = "2026-09-01 ~ 2026-09-30";
        private String currentUser = "Danny · 运营管理员";
        private String currentUserId = "MT-20873 · 已实名";
        private String currentUserAvatar = "贺";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getConsoleLabel() {
            return consoleLabel;
        }

        public void setConsoleLabel(String consoleLabel) {
            this.consoleLabel = consoleLabel;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getClusterStatus() {
            return clusterStatus;
        }

        public void setClusterStatus(String clusterStatus) {
            this.clusterStatus = clusterStatus;
        }

        public String getClusterLatency() {
            return clusterLatency;
        }

        public void setClusterLatency(String clusterLatency) {
            this.clusterLatency = clusterLatency;
        }

        public String getDateRange() {
            return dateRange;
        }

        public void setDateRange(String dateRange) {
            this.dateRange = dateRange;
        }

        public String getCurrentUser() {
            return currentUser;
        }

        public void setCurrentUser(String currentUser) {
            this.currentUser = currentUser;
        }

        public String getCurrentUserId() {
            return currentUserId;
        }

        public void setCurrentUserId(String currentUserId) {
            this.currentUserId = currentUserId;
        }

        public String getCurrentUserAvatar() {
            return currentUserAvatar;
        }

        public void setCurrentUserAvatar(String currentUserAvatar) {
            this.currentUserAvatar = currentUserAvatar;
        }
    }

    /** 跨域配置。 */
    public static class Cors {

        private boolean enabled = true;
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
        private String allowedMethods = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private boolean allowCredentials = true;
        private long maxAge = 3600L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /** 集群运维阈值。 */
    public static class Cluster {

        /** 端到端延迟 SLA 上限（毫秒）。 */
        private int slaLatencyMs = 250;
        /** 核心 API 设计容量（QPS）。 */
        private int designCapacityQps = 1200;
        /** 采样周期文案。 */
        private String samplingPeriod = "5s";
        /** 数据时延文案。 */
        private String dataDelay = "数据时延 < 1s";
        /** 单个容器折算的并发会话数（KPI 由容器数 × 该系数推导）。 */
        private int sessionsPerContainer = 180;
        /** 单个容器折算的 QPS（KPI 由容器数 × 该系数推导）。 */
        private int qpsPerContainer = 12;
        /** 无节点时的端到端平均延迟基准值（毫秒）。 */
        private int baseLatencyMs = 186;
        /** 节点健康度百分比（演示值，接入真实监控后由指标推导）。 */
        private double healthPct = 99.98;

        public int getSlaLatencyMs() {
            return slaLatencyMs;
        }

        public void setSlaLatencyMs(int slaLatencyMs) {
            this.slaLatencyMs = slaLatencyMs;
        }

        public int getDesignCapacityQps() {
            return designCapacityQps;
        }

        public void setDesignCapacityQps(int designCapacityQps) {
            this.designCapacityQps = designCapacityQps;
        }

        public int getSessionsPerContainer() {
            return sessionsPerContainer;
        }

        public void setSessionsPerContainer(int sessionsPerContainer) {
            this.sessionsPerContainer = sessionsPerContainer;
        }

        public int getQpsPerContainer() {
            return qpsPerContainer;
        }

        public void setQpsPerContainer(int qpsPerContainer) {
            this.qpsPerContainer = qpsPerContainer;
        }

        public int getBaseLatencyMs() {
            return baseLatencyMs;
        }

        public void setBaseLatencyMs(int baseLatencyMs) {
            this.baseLatencyMs = baseLatencyMs;
        }

        public double getHealthPct() {
            return healthPct;
        }

        public void setHealthPct(double healthPct) {
            this.healthPct = healthPct;
        }

        public String getSamplingPeriod() {
            return samplingPeriod;
        }

        public void setSamplingPeriod(String samplingPeriod) {
            this.samplingPeriod = samplingPeriod;
        }

        public String getDataDelay() {
            return dataDelay;
        }

        public void setDataDelay(String dataDelay) {
            this.dataDelay = dataDelay;
        }
    }

    /** 安全合规配置。 */
    public static class Security {

        /** 操作日志留存天数。 */
        private int logRetentionDays = 180;
        /** 登录是否校验点击式验证码（自动化联调 / 无障碍场景可临时关闭）。 */
        private boolean captchaEnabled = true;
        /** 连续口令失败达到该次数后临时锁定账号（0 表示不限制）。 */
        private int loginMaxAttempts = 5;
        /** 触发风控后的锁定时长（秒）。 */
        private long loginLockSeconds = 300;
        /**
         * 应用启动（含每次重启）时是否清空全部登录会话。
         *
         * <p>会话持久化在 auth_session 表，重启本身并不会让令牌失效；开启此项后
         * 每次启动都先清表，保证「重启 = 所有人重新登录」，避免长期挂起的令牌滞留。
         * 生产环境若希望重启不踢人，改为 false 即可。
         */
        private boolean invalidateOnStartup = true;

        public int getLogRetentionDays() {
            return logRetentionDays;
        }

        public void setLogRetentionDays(int logRetentionDays) {
            this.logRetentionDays = logRetentionDays;
        }

        public boolean isCaptchaEnabled() {
            return captchaEnabled;
        }

        public void setCaptchaEnabled(boolean captchaEnabled) {
            this.captchaEnabled = captchaEnabled;
        }

        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public long getLoginLockSeconds() {
            return loginLockSeconds;
        }

        public void setLoginLockSeconds(long loginLockSeconds) {
            this.loginLockSeconds = lockSecondsGuard(loginLockSeconds);
        }

        public boolean isInvalidateOnStartup() {
            return invalidateOnStartup;
        }

        public void setInvalidateOnStartup(boolean invalidateOnStartup) {
            this.invalidateOnStartup = invalidateOnStartup;
        }

        private static long lockSecondsGuard(long value) {
            return value < 0 ? 0 : value;
        }
    }

    /** 演示数据开关。 */
    public static class Data {

        /** 是否对实时指标做轻微随机扰动，用于演示"实时推送"效果。 */
        private boolean randomize = false;
        /** 首次启动且表为空时，是否自动灌入演示数据。 */
        private boolean autoInit = true;

        public boolean isRandomize() {
            return randomize;
        }

        public void setRandomize(boolean randomize) {
            this.randomize = randomize;
        }

        public boolean isAutoInit() {
            return autoInit;
        }

        public void setAutoInit(boolean autoInit) {
            this.autoInit = autoInit;
        }
    }
}
