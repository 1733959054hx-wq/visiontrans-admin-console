package com.gzu.adminconsole.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.dto.common.ToggleItem;
import com.gzu.adminconsole.model.AdSlot;
import com.gzu.adminconsole.model.AdminUser;
import com.gzu.adminconsole.model.AlarmEvent;
import com.gzu.adminconsole.model.AppUser;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.model.AuditPackage;
import com.gzu.adminconsole.model.ClusterNode;
import com.gzu.adminconsole.model.CustomerOrder;
import com.gzu.adminconsole.model.DeviceRecord;
import com.gzu.adminconsole.model.FrequencyCap;
import com.gzu.adminconsole.model.GlossaryTask;
import com.gzu.adminconsole.model.InvoiceApplication;
import com.gzu.adminconsole.model.MaterialAsset;
import com.gzu.adminconsole.model.MembershipPlan;
import com.gzu.adminconsole.model.ModelRelease;
import com.gzu.adminconsole.model.NavMenu;
import com.gzu.adminconsole.model.PermGroup;
import com.gzu.adminconsole.model.Permission;
import com.gzu.adminconsole.model.RefundRecord;
import com.gzu.adminconsole.model.ReleaseEvent;
import com.gzu.adminconsole.model.RoleDomain;
import com.gzu.adminconsole.model.SettlementRecord;
import com.gzu.adminconsole.model.UgcRecord;
import com.gzu.adminconsole.entity.AppSessionEntity;
import com.gzu.adminconsole.entity.BackupPolicyEntity;
import com.gzu.adminconsole.entity.CircuitBreakerEntity;
import com.gzu.adminconsole.entity.MerchantOnboardingEntity;
import com.gzu.adminconsole.entity.MetricSampleEntity;
import com.gzu.adminconsole.entity.ThirdPartyServiceEntity;
import com.gzu.adminconsole.entity.SystemLogEntity;
import com.gzu.adminconsole.repository.AdRepository;
import com.gzu.adminconsole.repository.ClusterRepository;
import com.gzu.adminconsole.repository.FinanceRepository;
import com.gzu.adminconsole.repository.MetricRepository;
import com.gzu.adminconsole.repository.ModerationRepository;
import com.gzu.adminconsole.repository.ModelRepository;
import com.gzu.adminconsole.repository.NavRepository;
import com.gzu.adminconsole.repository.OnboardingRepository;
import com.gzu.adminconsole.repository.OpsRepository;
import com.gzu.adminconsole.repository.SecurityRepository;
import com.gzu.adminconsole.repository.ThirdPartyRepository;

/**
 * 演示数据初始化器：应用启动后，若对应表为空则灌入一套演示数据。
 *
 * <p>关闭方式：application.properties 中设置 {@code admin-console.data.auto-init=false}。
 * 已有数据不会被覆盖，可直接在数据库里修改后重启验证。</p>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppProperties properties;
    private final ClusterRepository clusterRepository;
    private final ModelRepository modelRepository;
    private final ModerationRepository moderationRepository;
    private final AdRepository adRepository;
    private final SecurityRepository securityRepository;
    private final NavRepository navRepository;
    private final MetricRepository metricRepository;
    private final FinanceRepository financeRepository;
    private final OpsRepository opsRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final OnboardingRepository onboardingRepository;

    /**
     * 演示设备的关联 C 端账号（fingerprint → account）。
     *
     * <p>种子数据与老库回填共用，保证「移除指定设备登录态」在任何库上都能定位到账号与会话。
     */
    private static final Map<String, String> DEVICE_ACCOUNTS = Map.ofEntries(
            Map.entry("DEV-2A71-3C08", "林晚晴"),
            Map.entry("DEV-8F2A-9C31", "沈亦舟"),
            Map.entry("DEV-4D19-77E2", "顾清欢"),
            Map.entry("DEV-6B33-10AF", "苏念"),
            Map.entry("DEV-1E92-45D6", "陆之遥"),
            Map.entry("DEV-9C04-28B1", "江辞"),
            Map.entry("DEV-3F58-6AD9", "许栀"));

    /** 24 小时 QPS 形态基线（凌晨低谷、午晚高峰），主流程与漏斗指标补灌共用。 */
    private static final int[] QPS_SHAPE = {212, 186, 164, 148, 132, 124, 138, 186, 268, 342, 428, 486,
            528, 562, 548, 536, 586, 642, 708, 812, 860, 764, 588, 412};

    public DataInitializer(AppProperties properties,
                           ClusterRepository clusterRepository,
                           ModelRepository modelRepository,
                           ModerationRepository moderationRepository,
                           AdRepository adRepository,
                           SecurityRepository securityRepository,
                           NavRepository navRepository,
                           MetricRepository metricRepository,
                           FinanceRepository financeRepository,
                           OpsRepository opsRepository,
                           ThirdPartyRepository thirdPartyRepository,
                           OnboardingRepository onboardingRepository) {
        this.properties = properties;
        this.clusterRepository = clusterRepository;
        this.modelRepository = modelRepository;
        this.moderationRepository = moderationRepository;
        this.adRepository = adRepository;
        this.securityRepository = securityRepository;
        this.navRepository = navRepository;
        this.metricRepository = metricRepository;
        this.financeRepository = financeRepository;
        this.opsRepository = opsRepository;
        this.thirdPartyRepository = thirdPartyRepository;
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getData().isAutoInit()) {
            log.info("演示数据初始化已关闭（admin-console.data.auto-init=false），跳过");
            return;
        }
        initNav();
        initCluster();
        initAlarms();
        initModels();
        initModeration();
        initAuditPackages();
        initAds();
        initSecurity();
        initAppUsers();
        initAppSessions();
        initFinance();
        initOnboardings();
        initOps();
        initDependencies();
        initSysConfig();
        // 时序指标采样：KPI / 波形 / 曲线全部改为由这些落库数据统计得出
        initMetrics();
        // 历史演示数据迁移：管理员显示名 张小雨 → Danny（幂等）
        securityRepository.renameAdmin("张小雨", "Danny");
        // 始终确保后台管理员账号与默认口令存在（兼容老数据缺账号 / 口令字段的情况）
        initAdmins();
    }

    /**
     * 时序指标采样：灌入近 30 天的真实落库数据，供 KPI、延迟波形、QPS 曲线、趋势迷你图统计使用。
     * 固定随机种子，保证多次启动得到一致的数据；已有数据时不重复写入。
     */
    private void initMetrics() {
        if (metricRepository.count() > 0) {
            // 老库升级：仅补齐新增的曝光 / 点击 / 转化漏斗指标，已有的指标采样不重灌
            backfillAdFunnelMetrics();
            return;
        }
        // 24 小时 QPS 形态基线（凌晨低谷、午晚高峰）
        int[] qpsShape = QPS_SHAPE;
        // 端到端延迟管线分层：层名、渐变起止色、基准耗时
        String[] layers = {"帧抓取", "OCR 检测", "NMT 翻译", "空间渲染", "图层上屏"};
        String[] c1 = {"#1E3A8A", "#2563EB", "#0EA5E9", "#38BDF8", "#6366F1"};
        String[] c2 = {"#3B62C4", "#60A5FA", "#7DD3FC", "#BAE6FD", "#A5B4FC"};
        double[] layerBase = {18, 46, 62, 34, 16};
        String[] blockSources = {"华东 1（上海）", "华南 1（深圳）", "海外节点"};
        /** 每天波形采样点数。 */
        int wavePoints = 40;
        /** 生成天数。 */
        int days = 30;

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        // 固定种子：数据可复现，避免每次启动指标乱跳
        Random rnd = new Random(20260907L);
        List<MetricSampleEntity> rows = new ArrayList<>();

        for (int d = days - 1; d >= 0; d--) {
            String date = today.minusDays(d).format(df);
            // 当日整体水位因子，使每天数据不同但形态稳定
            double dayFactor = 0.92 + rnd.nextDouble() * 0.16;

            // 1) 24 小时 QPS
            for (int h = 0; h < 24; h++) {
                rows.add(new MetricSampleEntity(MetricRepository.MetricKey.CLUSTER_QPS_HOURLY, date,
                        String.format("%02d", h), h, Math.round(qpsShape[h] * dayFactor), null, null));
            }
            // 2) 管线分段耗时（当天口径）
            for (int i = 0; i < layers.length; i++) {
                rows.add(new MetricSampleEntity(MetricRepository.MetricKey.LATENCY_SEGMENT, date, layers[i], i,
                        round1(layerBase[i] * dayFactor), c1[i], c2[i]));
            }
            // 3) 延迟波形采样（层 × 点）
            for (int i = 0; i < layers.length; i++) {
                for (int p = 0; p < wavePoints; p++) {
                    double v = layerBase[i] * dayFactor
                            + layerBase[i] * 0.22 * Math.sin(p / 6.0 + i)
                            + layerBase[i] * 0.08 * Math.sin(p / 2.3 + i * 2);
                    rows.add(new MetricSampleEntity(MetricRepository.MetricKey.LATENCY_WAVE, date, layers[i], p,
                            Math.max(1, round1(v)), c1[i], c2[i]));
                }
            }
            // 4) 广告调度请求数 / 曝光 / 点击 / 转化（按小时，转化率链路可自洽）
            for (int h = 0; h < 24; h++) {
                long requests = Math.round(38000 * dayFactor * (0.6 + qpsShape[h] / 900.0));
                addAdFunnelRows(rows, date, h, requests, rnd, true);
            }
            // 5) 越权访问拦截事件（当天若干条，value = 1 便于 count / sum）
            int blocks = 4 + rnd.nextInt(6);
            for (int b = 0; b < blocks; b++) {
                rows.add(new MetricSampleEntity(MetricRepository.MetricKey.SECURITY_BLOCK, date,
                        blockSources[b % blockSources.length], b, 1, null, null));
            }
            // 6) 模型自动回滚事件
            int rollbacks = rnd.nextInt(3);
            for (int r = 0; r < rollbacks; r++) {
                rows.add(new MetricSampleEntity(MetricRepository.MetricKey.MODEL_ROLLBACK, date,
                        "NMT-Edge-zhXX", r, 1, null, null));
            }
            // 7) 单值指标：采样可用率 / 热更耗时 / 广告决策耗时与填充率
            rows.add(new MetricSampleEntity(MetricRepository.MetricKey.CLUSTER_AVAILABILITY, date, "可用性", 0,
                    round1(95.6 + rnd.nextDouble() * 1.6), null, null));
            rows.add(new MetricSampleEntity(MetricRepository.MetricKey.CLUSTER_DATA_DELAY_SECONDS, date, "数据时延",
                    0, round1(3.5 + rnd.nextDouble() * 1.3), null, null));
            rows.add(new MetricSampleEntity(MetricRepository.MetricKey.MODEL_HOTFIX_SECONDS, date, "热更耗时", 0,
                    round1(5.8 + rnd.nextDouble() * 1.4), null, null));
            rows.add(new MetricSampleEntity(MetricRepository.MetricKey.ADS_DECISION_MS, date, "决策耗时", 0,
                    round1(8.0 + rnd.nextDouble() * 1.4), null, null));
            rows.add(new MetricSampleEntity(MetricRepository.MetricKey.ADS_FILL_RATE, date, "填充率", 0,
                    round1(92.5 + rnd.nextDouble() * 3.5), null, null));
        }
        metricRepository.saveAll(rows);
        log.info("[初始化] metric_sample 已写入 {} 条时序采样（近 {} 天）", rows.size(), days);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * 追加某小时的一组广告漏斗采样：曝光 / 点击 / 转化（转化率链路可自洽）。
     *
     * @param withRequest 是否同时写入调度请求采样：老库补灌时 request 已存在，传 false 避免重复
     */
    private static void addAdFunnelRows(List<MetricSampleEntity> rows, String date, int hour, long requests,
                                        Random rnd, boolean withRequest) {
        long impressions = Math.round(requests * 3 * (0.94 + rnd.nextDouble() * 0.12));
        long clicks = Math.round(impressions * (0.058 + rnd.nextDouble() * 0.022));
        long conversions = Math.round(clicks * (0.092 + rnd.nextDouble() * 0.048));
        String label = String.format("%02d", hour);
        if (withRequest) {
            rows.add(new MetricSampleEntity(MetricRepository.MetricKey.ADS_REQUEST_HOURLY, date, label, hour,
                    requests, null, null));
        }
        rows.add(new MetricSampleEntity(MetricRepository.MetricKey.ADS_IMPRESSION_HOURLY, date, label, hour,
                impressions, null, null));
        rows.add(new MetricSampleEntity(MetricRepository.MetricKey.ADS_CLICK_HOURLY, date, label, hour,
                clicks, null, null));
        rows.add(new MetricSampleEntity(MetricRepository.MetricKey.ADS_CONVERSION_HOURLY, date, label, hour,
                conversions, null, null));
    }

    /**
     * 老库升级：为新增的曝光 / 点击 / 转化指标按天补齐采样。
     *
     * <p>三项指标彼此成套（转化率 = 转化 / 点击 / 曝光），任一缺失即整组重灌，
     * 与主流程共用同一形态基线，保证 KPI 口径一致。
     */
    private void backfillAdFunnelMetrics() {
        List<String> funnelKeys = List.of(MetricRepository.MetricKey.ADS_IMPRESSION_HOURLY,
                MetricRepository.MetricKey.ADS_CLICK_HOURLY, MetricRepository.MetricKey.ADS_CONVERSION_HOURLY);
        boolean missing = funnelKeys.stream().anyMatch(key -> metricRepository.countByKey(key) == 0);
        if (!missing) {
            // 幂等修复：即使指标已齐，也顺手清理历史版本补灌可能写入的重复请求采样
            int removed = metricRepository.dedupHourlySamples(MetricRepository.MetricKey.ADS_REQUEST_HOURLY);
            if (removed > 0) {
                log.info("[初始化] metric_sample 已清理重复调度请求采样 {} 条", removed);
            }
            return;
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        Random rnd = new Random(20260907L);
        List<MetricSampleEntity> rows = new ArrayList<>();
        int days = 30;
        for (int d = days - 1; d >= 0; d--) {
            String date = today.minusDays(d).format(df);
            double dayFactor = 0.92 + rnd.nextDouble() * 0.16;
            for (int h = 0; h < 24; h++) {
                long requests = Math.round(38000 * dayFactor * (0.6 + QPS_SHAPE[h] / 900.0));
                addAdFunnelRows(rows, date, h, requests, rnd, false);
            }
        }
        metricRepository.saveAll(rows);
        // 幂等修复：清理此前补灌版本可能写入的重复调度请求采样（无重复时影响 0 行）
        int removed = metricRepository.dedupHourlySamples(MetricRepository.MetricKey.ADS_REQUEST_HOURLY);
        log.info("[初始化] metric_sample 已补齐广告漏斗指标 {} 条（曝光 / 点击 / 转化，近 {} 天），"
                + "并清理重复调度请求采样 {} 条", rows.size(), days, removed);
    }

    /**
     * 侧边导航菜单：按需求清单的五个一级页面分组（系统管理 / 审核中心 / 广告运营 / 财务订单 / 监控运维），
     * 每条菜单携带可见角色，配合菜单权限配置实现按角色下发。
     *
     * <p>单一数据源：首次启动灌库与老库升级共用同一份菜单定义，保证版本间一致。
     */
    private static List<NavMenu> navMenus() {
        return List.of(
                // —— 系统管理 ——
                new NavMenu("a10", "fa-shield-halved", "系统管理", "Security & RBAC",
                        "用户管理、权限管理与系统配置",
                        "注册用户与登录设备台账 · 角色分配与菜单权限配置 · 接口限流 / 会话有效期等运行参数",
                        "系统管理", "System Admin", NavMenu.ALL_ROLES),
                // —— 审核中心 ——
                new NavMenu("a8", "fa-language", "内容审核", "Moderation",
                        "语种术语库审核与 UGC 风控中台",
                        "多语种术语库审核、AR 广告素材机审与 UGC 违规高亮拦截 · 平均处置耗时 82 ms",
                        "审核中心", "Audit Center", NavMenu.ALL_ROLES),
                new NavMenu("a7", "fa-brain", "版本管理", "Model Lifecycle",
                        "AI 模型生命周期与热更中心",
                        "端侧量化模型（INT8/FP16，最小 0.9MB）与云端大模型统一纳管 · 秒级热更 · 一键回滚",
                        "审核中心", "Audit Center", NavMenu.ALL_ROLES),
                // —— 广告运营 ——
                new NavMenu("a9", "fa-calendar-day", "广告运营", "Scheduler",
                        "全网广告位排期与调度引擎",
                        "12 类广告位库存甘特排期 · 场景×语种 eCPM 策略矩阵 · 曝光 / 点击 / 转化实时看板",
                        "广告运营", "Ads Operations", NavMenu.ALL_ROLES),
                // —— 财务订单 ——
                new NavMenu("a11", "fa-file-invoice-dollar", "财务订单", "Finance & Orders",
                        "财务订单与商户结算中心",
                        "C 端订单、异常订单处理、商户周期结算对账与 B 端发票审核 · 对账周期 T+1",
                        "财务订单", "Finance & Orders", NavMenu.ALL_ROLES),
                // —— 监控运维 ——
                new NavMenu("a6", "fa-satellite-dish", "监控运维", "Cluster Ops",
                        "集群态势感知与推演监控大盘",
                        "全网实时并发、端到端延迟拆解、QPS 吞吐与节点健康度 · 采样周期 5s · 数据时延 < 1s",
                        "监控运维", "Monitor & Ops", NavMenu.ALL_ROLES));
    }

    private void initNav() {
        if (navRepository.isEmpty()) {
            navRepository.saveAll(navMenus());
            log.info("[初始化] nav_menu 已写入 {} 条菜单（按一级页面分组）", navMenus().size());
            return;
        }
        // 老库升级：把菜单分组 / 文案 / 排序与当前版本对齐（不覆盖运营已配置的可见角色）
        List<NavMenu> menus = navMenus();
        int upgraded = 0;
        for (int i = 0; i < menus.size(); i++) {
            if (navRepository.upgradeMenu(menus.get(i), i)) {
                upgraded++;
            }
        }
        // 缺失的菜单（如老库缺 a11）单独补齐
        for (int i = 0; i < menus.size(); i++) {
            NavMenu menu = menus.get(i);
            if (!navRepository.existsById(menu.id())) {
                navRepository.insertOne(menu, i);
                log.info("[初始化] nav_menu 已补齐菜单 {}", menu.id());
            }
        }
        // 为既有菜单补上可见角色默认值，保证菜单权限配置可用
        navRepository.backfillGroupAndRoles();
        if (upgraded > 0) {
            log.info("[初始化] nav_menu 已按一级页面分组升级 {} 条菜单", upgraded);
        }
    }

    private void initCluster() {
        if (!clusterRepository.isEmpty()) {
            return;
        }
        List<ClusterNode> nodes = List.of(
                new ClusterNode("vt-shanghai-core-01", "华东 1（上海）", "主节点 · GPU 推理", 4, 62.4, 41.2, 186, "健康"),
                new ClusterNode("vt-shanghai-core-02", "华东 1（上海）", "主节点 · GPU 推理", 2, 58.9, 39.6, 192, "健康"),
                new ClusterNode("vt-shanghai-nmt-01", "华东 1（上海）", "NMT 翻译", 2, 71.8, 48.3, 178, "健康"),
                new ClusterNode("vt-shanghai-asr-01", "华东 1（上海）", "ASR 语音识别", 2, 66.2, 44.1, 204, "健康"),
                new ClusterNode("vt-shenzhen-dr-01", "华南 1（深圳）", "容灾节点 · CPU 兜底", 2, 83.6, 66.9, 246, "高负载"),
                new ClusterNode("vt-shenzhen-dr-02", "华南 1（深圳）", "容灾节点 · CPU 兜底", 2, 54.2, 37.1, 228, "健康"),
                new ClusterNode("vt-shanghai-gateway-01", "华东 1（上海）", "API 网关 / 鉴权", 1, 47.5, 32.8, 168, "健康"),
                new ClusterNode("vt-shanghai-cache-01", "华东 1（上海）", "缓存 / 模型分发", 1, 38.4, 25.6, 142, "健康"));
        List<AlarmEvent> alarms = List.of(
                AlarmEvent.of("14:22:08", "P2", "华南 1（深圳）容灾节点负载 83.6%，已自动扩容 2 个容器", "已自愈"),
                AlarmEvent.of("13:56:41", "P3", "主节点出口带宽利用率 78%，已启用限流降级预案", "已自愈"),
                AlarmEvent.of("13:18:02", "P1", "NMT 翻译服务 v3.8.2 灰度批次 B 错误率 0.42% 超阈值，已自动回滚", "已回滚"),
                AlarmEvent.of("12:04:33", "P3", "华南容灾节点磁盘水位 78%，触发日志清理任务", "已处理"),
                AlarmEvent.of("11:42:19", "P2", "主节点 GPU 显存碎片率 31%，已触发显存整理", "已处理"),
                AlarmEvent.of("10:26:57", "P3", "开屏广告素材 CDN 回源率上升至 6.8%，已预热缓存", "已自愈"));
        clusterRepository.saveNodes(nodes);
        clusterRepository.saveAlarms(alarms);
        log.info("[初始化] cluster_node / alarm_event 已写入 {} / {} 条", nodes.size(), alarms.size());
    }

    /**
     * 告警事件：在近 30 天内生成一批带真实日期时间的告警，覆盖 P1/P2/P3 与多种处置结果。
     *
     * <p>与 initCluster 中的少量固定样本不同，这里按固定随机种子批量生成，既有已闭环的
     * 历史告警，也保留少量"处理中 / 待处理"，便于验证筛选与状态分布；已有足够数据时跳过。
     */
    private void initAlarms() {
        // 兼容老数据：纯时间（"14:22:08"）补上当天日期，与时间列展示口径统一
        for (AlarmEvent old : clusterRepository.findAlarms()) {
            String time = old.time() == null ? "" : old.time().trim();
            if (time.matches("\\d{2}:\\d{2}(:\\d{2})?")) {
                clusterRepository.updateAlarm(new AlarmEvent(old.id(), DateRange.withDate(time), old.level(),
                        old.message(), old.result()));
            }
        }
        if (clusterRepository.countAlarms() >= 20) {
            return;
        }

        Random random = new Random(20260907L);
        String[] zones = {"华东 1（上海）", "华南 1（深圳）", "华北 2（北京）", "海外 · 新加坡"};
        String[] services = {"NMT 翻译服务", "OCR 检测服务", "ASR 语音识别", "API 网关", "空间渲染服务", "广告决策引擎"};
        String[] closedResults = {"已自愈", "已处理", "已回滚"};
        String[] openResults = {"处理中", "待处理"};
        LocalDate today = LocalDate.now();
        List<AlarmEvent> alarms = new ArrayList<>();

        for (int i = 0; i < 26; i++) {
            String zone = zones[random.nextInt(zones.length)];
            String service = services[random.nextInt(services.length)];
            int roll = random.nextInt(100);
            String level;
            String result;
            String message;
            if (roll < 15) {
                // P1：错误率超阈值触发自动回滚，少量仍在跟进
                level = "P1";
                result = roll % 3 == 0 ? "处理中" : "已回滚";
                message = String.format("%s %s v%d.%d.%d 灰度批次错误率 %.2f%% 超过 0.30%% 阈值，已触发自动回滚",
                        zone, service, 3 + random.nextInt(2), random.nextInt(9), random.nextInt(9),
                        0.32 + random.nextDouble() * 0.45);
            } else if (roll < 55) {
                // P2：资源水位告警，大部分已自愈，少量待处理
                level = "P2";
                result = roll % 5 == 0 ? "待处理" : closedResults[random.nextInt(closedResults.length)];
                message = String.format("%s 节点 CPU 利用率 %.1f%%，已自动扩容 %d 个容器",
                        zone, 78 + random.nextDouble() * 16, 1 + random.nextInt(3));
            } else {
                // P3：性能与容量类，基本已闭环
                level = "P3";
                result = roll % 11 == 0 ? openResults[random.nextInt(openResults.length)]
                        : closedResults[random.nextInt(closedResults.length)];
                message = String.format("%s %s 平均响应升至 %d ms，已启用限流降级预案",
                        zone, service, 180 + random.nextInt(130));
            }
            LocalDate day = today.minusDays(random.nextInt(30));
            String time = day + String.format(" %02d:%02d:%02d", random.nextInt(24), random.nextInt(60), random.nextInt(60));
            alarms.add(AlarmEvent.of(time, level, message, result));
        }

        // 最新的排最前：入库顺序即展示顺序（findAlarms 按 id 升序）
        alarms.sort(Comparator.comparing(AlarmEvent::time).reversed());
        clusterRepository.saveAlarms(alarms);
        log.info("[初始化] alarm_event 已生成 {} 条近 30 天告警，总计 {} 条", alarms.size(), clusterRepository.countAlarms());
    }

    private void initModels() {
        if (!modelRepository.isEmpty()) {
            return;
        }
        List<ModelRelease> models = List.of(
                new ModelRelease("DBNet-Tiny v4.2.1", "端侧 OCR", "INT8", "0.9 MB", "98.4%", 100, "全量"),
                new ModelRelease("CRNN-zhXX v3.1.0", "端侧 OCR", "INT8", "1.2 MB", "96.2%", 100, "全量"),
                new ModelRelease("NMT-Edge-zhXX v3.8.4", "端侧翻译", "INT8", "3.4 MB", "92.1%", 35, "灰度 B 观察中"),
                new ModelRelease("NMT-Cloud-zhXX v5.0.2", "云端翻译", "FP16", "86 MB", "—", 100, "全量"),
                new ModelRelease("Whisper-AR v2.3.0", "端侧 ASR", "INT8", "4.1 MB", "88.7%", 60, "灰度 C 未开始"),
                new ModelRelease("NMT-Cloud-enXX v5.0.2", "云端翻译", "FP16", "91 MB", "—", 100, "全量"),
                new ModelRelease("Manga-OCR v1.4.0", "端侧漫画 OCR", "INT8", "1.8 MB", "84.3%", 100, "全量"),
                new ModelRelease("SignLLM v0.9.0", "端侧手语", "FP16", "12 MB", "41.2%", 10, "灰度 A 已通过"),
                new ModelRelease("Spatial-AR v3.0.1", "端侧空间渲染", "INT8", "2.6 MB", "91.5%", 100, "全量"));
        List<ReleaseEvent> timeline = List.of(
                new ReleaseEvent("v3.8.4 批次 B 灰度", "2026-09-02 22:06", "NMT-Edge-zhXX 灰度比例提升至 35%，观察中", "amber"),
                new ReleaseEvent("v4.2.1 端侧全量", "2026-09-02 18:33", "DBNet-Tiny 端侧全量下发，OCR 准确率 +0.3pt", "green"),
                new ReleaseEvent("v3.8.3 自动回滚", "2026-09-01 14:12", "批次 B P99 超阈值，触发自动回滚（耗时 4.2s）", "rose"),
                new ReleaseEvent("v5.0.2 云端全量", "2026-08-30 09:00", "NMT-Cloud 双语对升级，BLEU +1.8", "green"),
                new ReleaseEvent("v3.8.0 灰度发布", "2026-08-28 11:20", "NMT-Edge 首批 10% 灰度开启", "amber"));
        List<ToggleItem> strategies = List.of(
                new ToggleItem("自动回滚阈值", true),
                new ToggleItem("夜间低峰窗口", true),
                new ToggleItem("弱网断点续传", true),
                new ToggleItem("强制全量推送", false));
        modelRepository.saveModels(models);
        modelRepository.saveTimeline(timeline);
        modelRepository.saveStrategies(strategies);
        modelRepository.saveSettings(35, 3);
        log.info("[初始化] model_release 已写入 {} 条", models.size());
    }

    private void initModeration() {
        if (!moderationRepository.isEmpty()) {
            return;
        }
        List<GlossaryTask> tasks = List.of(
                new GlossaryTask(null, "国际机场安检术语包 v3.1", "高", "领域：交通 · 海关", "李审核", "09-04 18:00", "待初审"),
                new GlossaryTask(null, "医疗处方拉丁文对照 v2.0", "中", "领域：医疗", "王审核", "09-05 10:00", "待初审"),
                new GlossaryTask(null, "动漫拟声词词典 v1.4", "低", "领域：文娱", "—", "—", "待初审"),
                new GlossaryTask(null, "航空管制通话术语 v5.2", "高", "置信度 96.4%", "陈复核", "09-04 12:00", "术语复核"),
                new GlossaryTask(null, "法律条款多语种 v3.0", "中", "置信度 91.2%", "赵复核", "09-04 15:00", "术语复核"),
                new GlossaryTask(null, "机场标识多语 v4.0", "—", "本月 +312", "—", "—", "已发布"),
                new GlossaryTask(null, "金融财报术语 v2.8", "—", "本月 +88", "—", "—", "已发布"));
        List<MaterialAsset> assets = List.of(
                new MaterialAsset(null, "AR 开屏 · 全站冷启动", "98.4%", "通过"),
                new MaterialAsset(null, "AR 街景 · 南京路商圈", "95.1%", "通过"),
                new MaterialAsset(null, "AR Banner · 教育季", "88.7%", "人工复审"),
                new MaterialAsset(null, "AR 激励视频 · 游戏联运", "82.3%", "人工复审"),
                new MaterialAsset(null, "AR 信息流 · 电商", "74.6%", "驳回"),
                new MaterialAsset(null, "AR 贴片 · 影视宣发", "69.2%", "驳回"));
        moderationRepository.saveTasks(tasks);
        moderationRepository.saveAssets(assets);

        List<UgcRecord.Segment> segments = List.of(
                new UgcRecord.Segment("用户评论：「加我", null),
                new UgcRecord.Segment("微信 xxx 领免费翻译", "rose"),
                new UgcRecord.Segment("，比官方便宜一半」", null),
                new UgcRecord.Segment("疑似人身攻击内容", "amber"));
        List<UgcRecord.Hit> hits = List.of(
                new UgcRecord.Hit("导流话术", 0.94),
                new UgcRecord.Hit("辱骂", 0.88));
        moderationRepository.saveUgc(new UgcRecord("UGC-CURRENT", "高危导流", segments, hits, "待处置"));
        moderationRepository.saveRefund(new RefundRecord("RO-20260902-0418", "¥ 128.00", "¥ 64.00", "部分退款 50%",
                "SLA 剩余 6h12m", "命中规则：额度已使用 68% · 离线包不可退 · 非质量问题", "待仲裁"));
        log.info("[初始化] glossary_task / material_asset / ugc_record 已写入");
    }

    private void initAds() {
        if (!adRepository.isEmpty()) {
            return;
        }
        List<AdSlot> slots = List.of(
                new AdSlot(null, "开屏 · 全站冷启动首帧", "已售罄", "#EF4444", "100%", 100, true),
                new AdSlot(null, "信息流 · 首页第 3 位", "部分售出", "#F59E0B", "76%", 76, true),
                new AdSlot(null, "AR 街景 · 南京路商圈", "部分售出", "#F59E0B", "68%", 68, true),
                new AdSlot(null, "激励视频 · 游戏联运", "预售锁定", "#2563EB", "预售", -1, true),
                new AdSlot(null, "Banner · 教育季专题", "空闲可购", "#10B981", "0%", 0, true),
                new AdSlot(null, "贴片 · 影视宣发", "已售罄", "#EF4444", "100%", 100, true),
                new AdSlot(null, "搜索 · 品牌专区", "部分售出", "#F59E0B", "54%", 54, true),
                new AdSlot(null, "Push · 时区推送", "空闲可购", "#10B981", "0%", 0, true),
                new AdSlot(null, "暂停位 · A/B 实验", "部分售出", "#F59E0B", "41%", 41, true),
                new AdSlot(null, "直播 · 带货挂件商品", "预售锁定", "#2563EB", "预售", -1, true),
                new AdSlot(null, "会员 · 免广告权益", "已售罄", "#EF4444", "100%", 100, true),
                new AdSlot(null, "海外 · 多语种联运", "空闲可购", "#10B981", "0%", 0, false));
        List<FrequencyCap> caps = List.of(
                new FrequencyCap("单用户日上限", "5 次 / 天", 10, 5),
                new FrequencyCap("单用户周上限", "20 次 / 周", 40, 20),
                new FrequencyCap("单会话上限", "3 次", 10, 3),
                new FrequencyCap("冷却间隔", "30 分钟", 60, 30),
                new FrequencyCap("AR 锚定点上限", "2 个 / 场景", 6, 2));
        List<ToggleItem> strategies = List.of(
                new ToggleItem("VIP 用户豁免", true),
                new ToggleItem("新用户保护期", true),
                new ToggleItem("跨端去重", true),
                new ToggleItem("弱网降级", false));
        adRepository.saveSlots(slots);
        adRepository.saveCaps(caps);
        adRepository.saveFreqStrategies(strategies);
        log.info("[初始化] ad_slot / frequency_cap 已写入 {} / {} 条", slots.size(), caps.size());
    }

    private void initSecurity() {
        if (!securityRepository.isEmpty()) {
            // 老库升级：为既有设备回填关联账号，保证「移除指定设备登录态」能定位到会话
            securityRepository.backfillDeviceAccounts(DEVICE_ACCOUNTS);
            return;
        }
        List<RoleDomain> roles = List.of(
                new RoleDomain("超级管理员 · Super Admin", "SUPER_ADMIN", "fa-user-tie", 6, List.of(
                        new PermGroup("集群运维组", 2, List.of(
                                new Permission("集群态势大盘查看", Permission.GRANTED),
                                new Permission("节点上下线 / 驱逐", Permission.GRANTED),
                                new Permission("熔断降级编排", Permission.GRANTED),
                                new Permission("容量推演与扩容", Permission.PARTIAL))),
                        new PermGroup("模型治理组", 2, List.of(
                                new Permission("模型版本发布", Permission.GRANTED),
                                new Permission("灰度比例配置", Permission.GRANTED),
                                new Permission("秒级热更 / 回滚", Permission.GRANTED),
                                new Permission("训练数据接入", Permission.PARTIAL))),
                        new PermGroup("安全审计组", 2, List.of(
                                new Permission("操作日志只读", Permission.GRANTED),
                                new Permission("异常设备封禁", Permission.GRANTED),
                                new Permission("RBAC 权限配置", Permission.GRANTED),
                                new Permission("密钥轮换", Permission.NONE))))),
                new RoleDomain("运营管理员 · Operations", "OPERATIONS", "fa-user-gear", 30, List.of(
                        new PermGroup("内容审核组", 18, List.of(
                                new Permission("术语库初审", Permission.GRANTED),
                                new Permission("术语库终审", Permission.PARTIAL),
                                new Permission("UGC 违规处置", Permission.GRANTED),
                                new Permission("素材机审复核", Permission.GRANTED))),
                        new PermGroup("广告运营组", 8, List.of(
                                new Permission("广告位排期编辑", Permission.GRANTED),
                                new Permission("频次策略配置", Permission.GRANTED),
                                new Permission("溢价审批", Permission.PARTIAL),
                                new Permission("素材上下架", Permission.GRANTED))),
                        new PermGroup("商户与结算组", 4, List.of(
                                new Permission("套餐与额度配置", Permission.GRANTED),
                                new Permission("用户退款审批", Permission.GRANTED),
                                new Permission("分销渠道管理", Permission.GRANTED),
                                new Permission("资金调拨", Permission.NONE))))),
                new RoleDomain("只读审计员 · Auditor", "AUDITOR", "fa-user-lock", 4, List.of(
                        new PermGroup("合规审计组", 4, List.of(
                                new Permission("全量日志只读", Permission.GRANTED),
                                new Permission("合规报表导出", Permission.GRANTED),
                                new Permission("任何写操作", Permission.NONE),
                                new Permission("权限配置", Permission.NONE))))));
        List<DeviceRecord> devices = List.of(
                new DeviceRecord("北京 · 联通", "61.135.169.105", "DEV-2A71-3C08", "1,286", 86, "正常", false,
                        DEVICE_ACCOUNTS.get("DEV-2A71-3C08")),
                new DeviceRecord("上海 · 电信", "101.86.106.29", "DEV-8F2A-9C31", "642", 92, "异常", false,
                        DEVICE_ACCOUNTS.get("DEV-8F2A-9C31")),
                new DeviceRecord("深圳 · 电信", "113.87.193.226", "DEV-4D19-77E2", "1,024", 74, "可疑", false,
                        DEVICE_ACCOUNTS.get("DEV-4D19-77E2")),
                new DeviceRecord("广州 · 移动", "183.14.132.8", "DEV-6B33-10AF", "486", 68, "正常", false,
                        DEVICE_ACCOUNTS.get("DEV-6B33-10AF")),
                new DeviceRecord("杭州 · 移动", "112.17.68.31", "DEV-1E92-45D6", "318", 81, "可疑", false,
                        DEVICE_ACCOUNTS.get("DEV-1E92-45D6")),
                new DeviceRecord("成都 · 电信", "171.212.201.87", "DEV-9C04-28B1", "204", 35, "正常", false,
                        DEVICE_ACCOUNTS.get("DEV-9C04-28B1")),
                new DeviceRecord("西安 · 联通", "117.36.202.114", "DEV-3F58-6AD9", "142", 29, "正常", false,
                        DEVICE_ACCOUNTS.get("DEV-3F58-6AD9")));
        List<MembershipPlan> plans = List.of(
                new MembershipPlan("免费体验版", "¥ 0", "注册即享 · 有效期 30 天", "每日 20 次 · 仅端侧模型",
                        "1,286,420", "72%"),
                new MembershipPlan("会员月卡", "¥ 28", "按自然月订阅 · 自动续费", "每日 500 次 · 含云端大模型",
                        "86,240", "85%"),
                new MembershipPlan("会员年卡", "¥ 268", "按年订阅 · 立省 ¥ 68", "每日 1,200 次 · 含 AR 空间广告免打扰",
                        "24,186", "46%"));
        List<AuditLogEntry> logs = List.of(
                AuditLogEntry.of("2026-09-03 14:22:08", "张小雨", "超级管理员", "集群运维组",
                        "节点上下线 / 驱逐", "将 vt-shanghai-core-01 节点置为维护模式", "203.208.60.12 · 上海", "成功"),
                AuditLogEntry.of("2026-09-03 13:41:16", "陈默", "超级管理员", "模型治理组",
                        "模型版本发布", "发布 NMT-Cloud-zhXX v5.0.2 至全量", "203.208.60.31 · 上海", "成功"),
                AuditLogEntry.of("2026-09-03 12:58:02", "王倩", "运营管理员", "内容审核组",
                        "UGC 违规处置", "批量封禁高危导流账号 12 个", "112.17.68.42 · 杭州", "成功"),
                AuditLogEntry.of("2026-09-03 11:32:50", "赵磊", "运营管理员", "广告运营组",
                        "溢价策略审批", "审批通过「AR 街景锚定」溢价 12%", "112.17.68.57 · 杭州", "成功"),
                AuditLogEntry.of("2026-09-03 10:05:34", "孙奇", "运营管理员", "商户与结算组",
                        "套餐与额度配置", "调整会员年卡翻译额度至 12,000 次", "101.86.106.88 · 上海", "成功"),
                AuditLogEntry.of("2026-09-03 09:47:12", "刘洋", "只读审计员", "—",
                        "日志导出", "导出近 7 日操作日志（只读）", "61.135.169.77 · 北京", "成功"),
                AuditLogEntry.of("2026-09-03 08:59:41", "张小雨", "超级管理员", "安全审计组",
                        "异常设备封禁", "封禁异常登录设备 DEV-8F2A-9C31", "203.208.60.12 · 上海", "成功"),
                AuditLogEntry.of("2026-09-02 22:31:07", "system", "系统自动任务", "—",
                        "自动扩缩容", "华南 1 容灾节点扩容 2 个容器", "10.0.0.24 · 内网", "成功"));
        // 地图点位不再单独灌数据：SecurityService#mapCard 已改为由设备台账实时派生
        securityRepository.saveRoles(roles);
        securityRepository.saveDevices(devices);
        securityRepository.savePlans(plans);
        securityRepository.saveAuditLogs(logs);
        initPolicies();
        log.info("[初始化] security_role / device_record / membership_plan / audit_log 已写入");
    }

    /** 初始化后台管理员账号（KPI「管理员账号总数」由其真实统计得出）。 */
    private void initAdmins() {
        // 幂等刷新：已有时只补账号 / 口令，不覆盖用户后续修改
        List.of(
                new AdminUser(null, "Danny", "超级管理员", "集群运维组", "138****2043", "启用", "2026-09-03 14:22", "admin"),
                new AdminUser(null, "陈默", "超级管理员", "模型治理组", "139****8812", "启用", "2026-09-03 13:41", "chenmo"),
                new AdminUser(null, "王倩", "运营管理员", "内容审核组", "150****3391", "启用", "2026-09-03 12:58", "wangqian"),
                new AdminUser(null, "赵磊", "运营管理员", "广告运营组", "186****7720", "启用", "2026-09-03 11:32", "zhaolei"),
                new AdminUser(null, "孙奇", "运营管理员", "商户与结算组", "133****5590", "启用", "2026-09-03 10:05", "sunqi"),
                new AdminUser(null, "刘洋", "只读审计员", "合规审计组", "187****1186", "停用", "2026-09-03 09:47", "liuyang"))
                .forEach(securityRepository::refreshAdmin);
        log.info("[初始化] admin_user 已就绪（6 条，账号口令已确保存在）");
    }

    /** 初始化安全策略开关。 */
    private void initPolicies() {
        if (!securityRepository.policiesEmpty()) {
            return;
        }
        securityRepository.savePolicies(List.of(
                new ToggleItem("异地登录二次验证", true),
                new ToggleItem("越权访问自动阻断", true),
                new ToggleItem("高危操作双人复核", true),
                new ToggleItem("审计日志区块链存证", true),
                new ToggleItem("导出脱敏", false)));
        log.info("[初始化] security 策略开关已写入 5 条");
    }

    /** 近 days 天前的 "yyyy-MM-dd HH:mm" 时间文案。 */
    private static String daysAgo(int days, String hm) {
        return LocalDate.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + hm;
    }

    /** 初始化财务订单 / 商户结算 / 发票申请演示数据。 */
    private void initFinance() {
        if (financeRepository.ordersEmpty()) {
            List<CustomerOrder> orders = List.of(
                    new CustomerOrder(null, "ORD-20260907-0001", "林晚晴", "会员", "¥ 28.00",
                            CustomerOrder.STATUS_PAID, daysAgo(2, "09:12"), "会员月卡 · 微信支付"),
                    new CustomerOrder(null, "ORD-20260906-0002", "沈亦舟", "课程", "¥ 199.00",
                            CustomerOrder.STATUS_PAID, daysAgo(3, "14:35"), "AR 开发实战营 · 第 3 期"),
                    new CustomerOrder(null, "ORD-20260905-0003", "顾清欢", "数字内容", "¥ 12.00",
                            CustomerOrder.STATUS_PAID, daysAgo(4, "20:08"), "离线翻译包 · 日语"),
                    new CustomerOrder(null, "ORD-20260904-0004", "苏念", "会员", "¥ 268.00",
                            CustomerOrder.STATUS_PAID, daysAgo(5, "11:47"), "会员年卡 · 支付宝"),
                    new CustomerOrder(null, "ORD-20260906-0005", "陆之遥", "课程", "¥ 99.00",
                            CustomerOrder.STATUS_PAID, daysAgo(3, "08:22"), "术语库共建课"),
                    new CustomerOrder(null, "ORD-20260903-0006", "江辞", "数字内容", "¥ 30.00",
                            CustomerOrder.STATUS_PAID, daysAgo(6, "18:55"), "AR 滤镜素材包"),
                    new CustomerOrder(null, "ORD-20260908-0007", "温叙", "会员", "¥ 28.00",
                            CustomerOrder.STATUS_PENDING, daysAgo(1, "22:31"), "会员月卡 · 待唤起支付"),
                    new CustomerOrder(null, "ORD-20260907-0008", "纪云深", "课程", "¥ 199.00",
                            CustomerOrder.STATUS_ABNORMAL, daysAgo(2, "16:40"), "支付回调超时待核查"),
                    new CustomerOrder(null, "ORD-20260905-0009", "许栀", "数字内容", "¥ 18.00",
                            CustomerOrder.STATUS_ABNORMAL, daysAgo(4, "13:02"), "疑似重复扣款已冻结"),
                    new CustomerOrder(null, "ORD-20260904-0010", "黎照", "会员", "¥ 28.00",
                            CustomerOrder.STATUS_REFUNDING, daysAgo(5, "10:18"), "7 天无理由退款申请中"));
            financeRepository.saveOrders(orders);
            log.info("[初始化] customer_order 已写入 {} 条", orders.size());
        }
        if (financeRepository.settlementsEmpty()) {
            List<SettlementRecord> settlements = List.of(
                    new SettlementRecord(null, "晨曦文化", "2026-08", 1286, "¥ 86,240.00", "¥ 4,312.00",
                            SettlementRecord.STATUS_PENDING),
                    new SettlementRecord(null, "星野动漫", "2026-08", 862, "¥ 52,180.00", "¥ 2,609.00",
                            SettlementRecord.STATUS_PENDING),
                    new SettlementRecord(null, "蓝湾科技", "2026-08", 512, "¥ 33,412.00", "¥ 1,670.60",
                            SettlementRecord.STATUS_PENDING),
                    new SettlementRecord(null, "青禾教育", "2026-07", 428, "¥ 28,905.00", "¥ 1,445.25",
                            SettlementRecord.STATUS_RECONCILED),
                    new SettlementRecord(null, "云梦互娱", "2026-07", 356, "¥ 21,360.00", "¥ 1,068.00",
                            SettlementRecord.STATUS_SETTLED));
            financeRepository.saveSettlements(settlements);
            log.info("[初始化] settlement_record 已写入 {} 条", settlements.size());
        }
        if (financeRepository.invoicesEmpty()) {
            List<InvoiceApplication> invoices = List.of(
                    new InvoiceApplication(null, "INV-20260901-001", "晨曦文化传媒有限公司", "增值税专用发票",
                            "91330106MA2H3K7X0A", "¥ 86,240.00", InvoiceApplication.STATUS_PENDING,
                            daysAgo(6, "10:20")),
                    new InvoiceApplication(null, "INV-20260902-002", "星野动漫工作室", "增值税普通发票",
                            "91350211M0001WQ25B", "¥ 52,180.00", InvoiceApplication.STATUS_PENDING,
                            daysAgo(5, "15:46")),
                    new InvoiceApplication(null, "INV-20260828-011", "蓝湾科技有限公司", "增值税专用发票",
                            "91440300MA5DKQ8L7C", "¥ 33,412.00", InvoiceApplication.STATUS_ISSUED,
                            daysAgo(12, "09:31")),
                    new InvoiceApplication(null, "INV-20260830-015", "青禾教育咨询有限公司", "增值税专用发票",
                            "91330106TAXNO0FAIL", "¥ 28,905.00", InvoiceApplication.STATUS_REJECTED,
                            daysAgo(10, "17:05")));
            financeRepository.saveInvoices(invoices);
            log.info("[初始化] invoice_apply 已写入 {} 条", invoices.size());
        }
    }

    /**
     * 初始化 C 端登录会话：按设备台账派生，使「查看用户登录设备列表 / 移除指定设备登录态」可真实操作。
     *
     * <p>会话有效期取自系统参数 sys.session_ttl（默认 24 小时），前 4 台设备各挂 1~2 个有效会话。
     */
    private void initAppSessions() {
        if (!securityRepository.appSessionsEmpty()) {
            return;
        }
        int ttlHours = 24;
        try {
            ttlHours = Integer.parseInt(securityRepository.getSetting(SecurityRepository.KEY_SESSION_TTL));
        } catch (NumberFormatException ignored) {
            // 参数缺失 / 非数字时用默认值
        }
        List<DeviceRecord> devices = securityRepository.findDevices();
        List<AppSessionEntity> sessions = new ArrayList<>();
        int[] counts = {2, 1, 2, 1, 1, 0, 0};
        for (int i = 0; i < devices.size() && i < counts.length; i++) {
            DeviceRecord device = devices.get(i);
            for (int s = 0; s < counts[i]; s++) {
                LocalDateTime loginAt = LocalDateTime.now().minusHours(2L * (s + 1) + i * 3L);
                sessions.add(new AppSessionEntity(
                        "APP-" + device.fingerprint().replace("DEV-", "") + "-" + s,
                        device.account(), device.fingerprint(), loginAt,
                        loginAt.plusHours(ttlHours)));
            }
        }
        securityRepository.saveAppSessions(sessions);
        log.info("[初始化] app_session 已写入 {} 条有效登录会话", sessions.size());
    }

    /** 初始化 C 端用户演示数据（注册来源 / 会员状态全覆盖，含 1 条停用）。 */
    private void initAppUsers() {
        if (!securityRepository.appUsersEmpty()) {
            return;
        }
        List<AppUser> users = List.of(
                new AppUser(null, "林晚晴", "手机号", "会员月卡", daysAgo(230, "10:12"), daysAgo(0, "09:26"), "正常"),
                new AppUser(null, "沈亦舟", "微信", "会员年卡", daysAgo(412, "21:40"), daysAgo(0, "08:57"), "正常"),
                new AppUser(null, "顾清欢", "QQ", "免费体验", daysAgo(12, "19:05"), daysAgo(1, "22:31"), "正常"),
                new AppUser(null, "苏念", "Apple", "会员月卡", daysAgo(96, "08:33"), daysAgo(1, "20:14"), "正常"),
                new AppUser(null, "陆之遥", "手机号", "免费体验", daysAgo(5, "13:27"), daysAgo(2, "18:46"), "正常"),
                new AppUser(null, "江辞", "微信", "会员过期", daysAgo(300, "16:59"), daysAgo(6, "11:08"), "正常"),
                new AppUser(null, "温叙", "手机号", "会员年卡", daysAgo(520, "11:22"), daysAgo(0, "07:41"), "正常"),
                new AppUser(null, "纪云深", "QQ", "会员月卡", daysAgo(154, "23:48"), daysAgo(3, "15:52"), "正常"),
                new AppUser(null, "许栀", "微信", "免费体验", daysAgo(28, "12:15"), daysAgo(1, "19:37"), "正常"),
                new AppUser(null, "黎照", "Apple", "会员过期", daysAgo(365, "17:26"), daysAgo(14, "09:03"), "正常"),
                new AppUser(null, "洛白", "手机号", "会员月卡", daysAgo(77, "09:54"), daysAgo(0, "21:19"), "正常"),
                new AppUser(null, "闻人夏", "QQ", "免费体验", daysAgo(41, "14:41"), daysAgo(20, "10:30"), "停用"));
        securityRepository.saveAppUsers(users);
        log.info("[初始化] app_user 已写入 {} 条", users.size());
    }

    /** 初始化语种包 / 课程知识包审核演示数据。 */
    private void initAuditPackages() {
        if (!moderationRepository.packagesEmpty()) {
            return;
        }
        List<AuditPackage> packages = List.of(
                new AuditPackage(null, "语种包", "英语种翻译包 EN-Core v5.2", "端侧团队",
                        "词条 12,480 · 覆盖日常场景", AuditPackage.STATUS_PUBLISHED),
                new AuditPackage(null, "语种包", "日语种翻译包 JA-Edge v3.8", "术语组",
                        "词条 9,120 · 新增动漫拟声词", AuditPackage.STATUS_PUBLISHED),
                new AuditPackage(null, "语种包", "韩语种翻译包 KO-Beta v1.2", "众包平台",
                        "词条 3,640 · 待复核 128", AuditPackage.STATUS_PENDING),
                new AuditPackage(null, "课程知识包", "航空出行知识包 v2.1", "内容运营",
                        "知识点 486 · 场景：机场/机舱", AuditPackage.STATUS_PENDING),
                new AuditPackage(null, "课程知识包", "医疗问诊知识包 v1.6", "合规审核",
                        "知识点 320 · 敏感词已过滤", AuditPackage.STATUS_PUBLISHED),
                new AuditPackage(null, "课程知识包", "餐饮美食知识包 v0.9", "众包平台",
                        "知识点 154 · 术语置信度 82%", AuditPackage.STATUS_REJECTED));
        moderationRepository.savePackages(packages);
        log.info("[初始化] audit_package 已写入 {} 条", packages.size());
    }

    /** 初始化监控运维演示数据：系统日志 / 熔断降级策略 / 备份策略。 */
    private void initOps() {
        if (opsRepository.countSysLogs() == 0L) {
            List<SystemLogEntity> logs = buildSysLogs();
            opsRepository.saveSysLogs(logs);
            log.info("[初始化] system_log 已写入 {} 条", logs.size());
        }
        if (opsRepository.countBreakers() == 0L) {
            List<CircuitBreakerEntity> breakers = List.of(
                    new CircuitBreakerEntity("NMT 翻译服务", "慢调用比例", "错误率 > 0.5%", "关闭", true, 0),
                    new CircuitBreakerEntity("OCR 检测服务", "异常比例", "错误率 > 1.0%", "开启", true, 1),
                    new CircuitBreakerEntity("ASR 语音识别", "慢调用比例", "P99 > 800 ms", "半开", true, 2),
                    new CircuitBreakerEntity("API 网关", "并发控制", "并发 > 5,000", "降级中", true, 3),
                    new CircuitBreakerEntity("空间渲染服务", "异常数", "每分钟异常 > 50", "关闭", false, 4));
            opsRepository.saveBreakers(breakers);
            log.info("[初始化] circuit_breaker 已写入 {} 条", breakers.size());
        }
        if (opsRepository.countBackups() == 0L) {
            List<BackupPolicyEntity> backups = List.of(
                    new BackupPolicyEntity("集群配置快照", "每日", "保留 30 天", "对象存储 OSS", true, 0),
                    new BackupPolicyEntity("审计日志归档", "每周", "保留 180 天", "冷归档存储", true, 1),
                    new BackupPolicyEntity("模型产物备份", "每次发布", "保留最近 5 版", "对象存储 OSS", true, 2),
                    new BackupPolicyEntity("业务数据库全量", "每日", "保留 7 天", "本地 NAS", false, 3));
            opsRepository.saveBackups(backups);
            log.info("[初始化] backup_policy 已写入 {} 条", backups.size());
        }
    }

    /** 构造近 3 天的系统日志（应用日志 / 错误日志 / 模型推理 三类各 6 条）。 */
    private List<SystemLogEntity> buildSysLogs() {
        record LogSeed(int day, String hm, String level, String category, String source, String message) {
        }
        List<LogSeed> seeds = List.of(
                new LogSeed(0, "09:41:22", "INFO", "应用日志", "api-gateway", "GET /api/cluster/overview 200 · 42 ms"),
                new LogSeed(0, "09:38:07", "INFO", "应用日志", "auth-service", "管理员 Danny 登录成功（双因子校验通过）"),
                new LogSeed(0, "08:55:40", "WARN", "应用日志", "task-scheduler", "夜间低峰窗口任务延迟 3 分钟执行"),
                new LogSeed(1, "22:10:18", "INFO", "应用日志", "api-gateway", "GET /api/ads/overview 200 · 55 ms"),
                new LogSeed(1, "20:44:51", "INFO", "应用日志", "export-worker", "审计日志导出任务完成（1,286 条）"),
                new LogSeed(2, "18:32:09", "WARN", "应用日志", "api-gateway", "华东 1 节点连接池使用率 82%，接近阈值"),
                new LogSeed(0, "09:12:33", "ERROR", "错误日志", "payment-callback", "订单回调处理超时（ORD-20260907-0008）"),
                new LogSeed(0, "07:58:14", "ERROR", "错误日志", "model-dispatch", "NMT-Edge 批次 B 推理错误率 0.42% 超阈值"),
                new LogSeed(1, "21:26:45", "WARN", "错误日志", "cdn-edge", "AR 素材回源率 6.8%，已触发缓存预热"),
                new LogSeed(1, "14:03:29", "ERROR", "错误日志", "payment-callback", "疑似重复扣款已冻结（ORD-20260905-0009）"),
                new LogSeed(2, "11:47:02", "ERROR", "错误日志", "storage-sync", "备份同步失败 1 次，已自动重试成功"),
                new LogSeed(2, "03:15:57", "WARN", "错误日志", "api-gateway", "越权访问拦截 4 次，来源 112.17.68.42"),
                new LogSeed(0, "09:30:11", "INFO", "模型推理", "nmt-edge", "v3.8.4 INT8 推理平均耗时 61 ms · QPS 486"),
                new LogSeed(0, "08:20:36", "INFO", "模型推理", "ocr-tiny", "DBNet-Tiny v4.2.1 灰度批次识别准确率 98.4%"),
                new LogSeed(1, "19:52:44", "WARN", "模型推理", "asr-whisper", "Whisper-AR v2.3.0 噪声场景置信度下降至 81%"),
                new LogSeed(1, "10:15:23", "INFO", "模型推理", "nmt-cloud", "云端双语对 BLEU +1.8 · 热更耗时 5.6 s"),
                new LogSeed(2, "16:38:50", "INFO", "模型推理", "spatial-ar", "空间渲染模型 v3.0.1 帧率稳定 60 fps"),
                new LogSeed(2, "05:04:12", "ERROR", "模型推理", "nmt-edge", "v3.8.2 灰度批次自动回滚（耗时 4.2 s）"));
        List<SystemLogEntity> logs = new ArrayList<>();
        for (LogSeed seed : seeds) {
            logs.add(new SystemLogEntity(daysAgo(seed.day, seed.hm), seed.level, seed.category, seed.source,
                    seed.message, logs.size()));
        }
        return logs;
    }

    /** 初始化商户入驻申请（资质提交 / 合同签署的后台审核对象）。 */
    private void initOnboardings() {
        if (!onboardingRepository.isEmpty()) {
            return;
        }
        onboardingRepository.saveAll(List.of(
                new MerchantOnboardingEntity("MO-20260907-001", "晨曦文化传媒有限公司",
                        "91330106MA2H3K7X0A", "周晨", "138-0013-8000",
                        "营业执照 · 广播电视节目制作经营许可证", "已签署", "待审核",
                        daysAgo(2, "10:12"), "—", ""),
                new MerchantOnboardingEntity("MO-20260908-002", "星野动漫工作室",
                        "91350211M0001WQ25B", "叶星野", "139-2200-1188",
                        "营业执照 · 著作权登记证书", "已签署", "待审核",
                        daysAgo(1, "15:36"), "—", ""),
                new MerchantOnboardingEntity("MO-20260906-003", "青禾教育科技",
                        "91440300MA5DKQ8L7C", "许青禾", "135-6688-2020",
                        "营业执照 · 办学许可证", "待签署", "待审核",
                        daysAgo(3, "09:04"), "—", "合同已发送 H5 签署链接，等待商户确认"),
                new MerchantOnboardingEntity("MO-20260905-004", "蓝湾科技有限公司",
                        "91440300MA5DKQ8L7D", "林蓝湾", "186-8899-6677",
                        "营业执照", "已签署", "已通过",
                        daysAgo(4, "11:22"), "Danny", "资质齐全 · 合同已签署，准予入驻"),
                new MerchantOnboardingEntity("MO-20260904-005", "云梦互娱",
                        "91330106MA2H3K7X0B", "秦云梦", "137-5566-3344",
                        "营业执照（副本模糊）", "已签署", "已驳回",
                        daysAgo(5, "17:48"), "Danny", "资质材料不清晰，请补充后重新提交")));
        log.info("[初始化] merchant_onboarding 已写入 5 条入驻申请");
    }

    /**
     * 初始化核心服务与第三方接口台账：状态与耗时为最近一次拨测结果，
     * 开机后可点「拨测」发起真实 HTTP 探测刷新。
     */
    private void initDependencies() {
        if (!thirdPartyRepository.isEmpty()) {
            return;
        }
        DateTimeFormatter stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String checkedAt = LocalDateTime.now().minusMinutes(3).format(stamp);
        thirdPartyRepository.saveAll(List.of(
                new ThirdPartyServiceEntity("平台 API 网关", "核心服务",
                        "http://localhost:8080/api/meta/system", 2000, "正常", 168, 99.98, checkedAt,
                        "自拨测 · 网关与鉴权链路", 0),
                new ThirdPartyServiceEntity("端侧模型分发 CDN", "核心服务",
                        "https://cdn.jsdelivr.net", 3000, "正常", 212, 99.86, checkedAt,
                        "INT8 / FP16 量化模型分发", 0),
                new ThirdPartyServiceEntity("NMT 云端翻译引擎", "翻译引擎",
                        "https://api-free.deepl.com", 3000, "正常", 342, 99.72, checkedAt,
                        "长句与专业领域译文主力", 0),
                new ThirdPartyServiceEntity("ASR 语音识别服务", "语音识别",
                        "https://www.google.com", 3000, "正常", 268, 99.65, checkedAt,
                        "会议 / 语音翻译实时转写", 0),
                new ThirdPartyServiceEntity("OCR 文字识别服务", "翻译引擎",
                        "https://cdn.jsdelivr.net", 3000, "正常", 296, 99.41, checkedAt,
                        "实时画面与图片文字检测", 0),
                new ThirdPartyServiceEntity("地理编码服务（腾讯 LBS）", "地理编码",
                        "https://apis.map.qq.com", 2500, "降级", 486, 98.92, checkedAt,
                        "高峰期偶发超时，已启用 Open-Meteo 兜底", 0),
                new ThirdPartyServiceEntity("支付渠道网关", "支付渠道",
                        "https://api.mch.weixin.qq.com", 3000, "正常", 305, 99.95, checkedAt,
                        "会员 / 课程订单收单", 0)));
        log.info("[初始化] third_party_service 已写入 7 条依赖服务台账");
    }

    /** 初始化系统配置：运行参数（缺省才写入，不覆盖运营调整）+ C 端功能开关。 */
    private void initSysConfig() {
        initSettingIfAbsent(SecurityRepository.KEY_RATE_LIMIT, SecurityRepository.DEFAULT_RATE_LIMIT);
        initSettingIfAbsent(SecurityRepository.KEY_SESSION_TTL, SecurityRepository.DEFAULT_SESSION_TTL);
        initSettingIfAbsent(OpsRepository.KEY_THRESHOLD_CPU, "85");
        initSettingIfAbsent(OpsRepository.KEY_THRESHOLD_MEM, "80");
        initSettingIfAbsent(OpsRepository.KEY_THRESHOLD_GPU, "90");
        log.info("[初始化] app_setting 系统参数已就绪（sys.rate_limit=600 · sys.session_ttl=24 · ops.threshold=85/80/90）");
        if (securityRepository.togglesEmpty(SecurityRepository.GROUP_FEATURES)) {
            securityRepository.saveToggles(SecurityRepository.GROUP_FEATURES, List.of(
                    new ToggleItem("实时画面翻译", true),
                    new ToggleItem("AR 虚实融合", true),
                    new ToggleItem("广告调度引擎", true),
                    new ToggleItem("会议翻译", true),
                    new ToggleItem("悬浮翻译窗", true),
                    new ToggleItem("语音播报", false)));
            log.info("[初始化] features 功能开关已写入 6 条");
        }
    }

    /** 设置项缺省时写入默认值（已存在则保留运营修改，幂等）。 */
    private void initSettingIfAbsent(String key, String defaultValue) {
        if (securityRepository.getSetting(key) == null) {
            securityRepository.setSetting(key, defaultValue);
        }
    }
}
