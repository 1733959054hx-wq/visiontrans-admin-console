package com.gzu.adminconsole.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.dto.cluster.DependencyVO;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.entity.ThirdPartyServiceEntity;
import com.gzu.adminconsole.repository.ThirdPartyRepository;

/**
 * 核心服务与第三方接口可用性监控 ViewModel 层。
 *
 * <p>拨测走真实 HTTP 请求（HEAD 优先，服务端不支持时回退 GET），
 * 结果写入 third_party_service 并即时反映到监控运维页。
 */
@Service
public class DependencyService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 可用率滑动平滑系数：新结果权重 0.2，历史权重 0.8。 */
    private static final double SMOOTHING = 0.2;
    /** 判定为「降级」的可用率下限（%）。 */
    private static final double DEGRADED_RATE = 99.0;

    private final ThirdPartyRepository repository;
    /** 定时拨测是否开启（由配置注入，仅用于前端展示当前模式）。 */
    private final boolean autoProbe;

    public DependencyService(ThirdPartyRepository repository,
            @org.springframework.beans.factory.annotation.Value(
                    "${admin-console.dependency.auto-probe:false}") boolean autoProbe) {
        this.repository = repository;
        this.autoProbe = autoProbe;
    }

    /** 依赖服务明细行（供集群大盘直接内联）。 */
    public List<DependencyVO.DependencyRow> rows() {
        return repository.findAll().stream().map(DependencyService::toRow).toList();
    }

    /** 依赖服务监控视图（含汇总数量）。 */
    public DependencyVO dependencies() {
        List<DependencyVO.DependencyRow> rows = rows();
        String latest = rows.stream()
                .map(DependencyVO.DependencyRow::lastCheck)
                .filter(text -> text != null && !text.isBlank() && !"—".equals(text))
                .reduce((a, b) -> b)
                .orElse("—");
        return new DependencyVO(rows, autoProbe ? "auto" : "manual", latest);
    }

    /** 对单个依赖服务发起一次真实拨测。 */
    public ActionResultVO probe(Long id) {
        ThirdPartyServiceEntity entity = repository.findById(id);
        if (entity == null) {
            throw new BusinessException("未找到依赖服务 #" + id);
        }
        ProbeResult result = doProbe(entity.getEndpoint(), entity.getTimeoutMs());
        apply(entity, result);
        repository.saveProbeResult(entity);
        return ActionResultVO.ok("「" + entity.getName() + "」拨测完成：" + entity.getStatus() + " · "
                + entity.getLatencyMs() + "ms", String.valueOf(id));
    }

    /** 批量拨测（定时任务 / 一键拨测）。 */
    public void probeAll() {
        for (ThirdPartyServiceEntity entity : repository.findAll()) {
            try {
                apply(entity, doProbe(entity.getEndpoint(), entity.getTimeoutMs()));
                repository.saveProbeResult(entity);
            } catch (RuntimeException ignored) {
                // 单个依赖拨测异常不影响其余依赖
            }
        }
    }

    private static void apply(ThirdPartyServiceEntity entity, ProbeResult result) {
        entity.setLatencyMs(result.latencyMs());
        entity.setLastCheck(LocalDateTime.now().format(FORMATTER));
        double base = entity.getSuccessRate() <= 0 ? (result.ok() ? 100.0 : 0.0) : entity.getSuccessRate();
        double rate = base * (1 - SMOOTHING) + (result.ok() ? 100.0 : 0.0) * SMOOTHING;
        entity.setSuccessRate(Math.round(rate * 100.0) / 100.0);
        if (!result.ok()) {
            entity.setStatus("不可用");
            entity.setRemark(result.detail());
        } else if (rate < DEGRADED_RATE) {
            entity.setStatus("降级");
            entity.setRemark("近 24 小时可用率 " + String.format("%.2f", rate) + "%");
        } else {
            entity.setStatus("正常");
            entity.setRemark(result.detail());
        }
    }

    private static DependencyVO.DependencyRow toRow(ThirdPartyServiceEntity entity) {
        String status = entity.getStatus() == null ? "未拨测" : entity.getStatus();
        return new DependencyVO.DependencyRow(entity.getId(), entity.getName(), entity.getCategory(),
                entity.getEndpoint(), entity.getTimeoutMs(), status, toneOf(status), entity.getLatencyMs(),
                entity.getSuccessRate(), entity.getLastCheck() == null ? "—" : entity.getLastCheck(),
                entity.getRemark() == null ? "" : entity.getRemark());
    }

    private static String toneOf(String status) {
        return switch (status) {
            case "正常" -> "green";
            case "降级" -> "amber";
            case "不可用" -> "red";
            default -> "slate";
        };
    }

    /** 真实 HTTP 拨测：HEAD 优先，服务端不支持时回退 GET。 */
    private static ProbeResult doProbe(String endpoint, int timeoutMs) {
        if (endpoint == null || endpoint.isBlank()) {
            return new ProbeResult(false, 0, "未配置拨测地址");
        }
        int timeout = Math.max(500, timeoutMs);
        long begin = System.nanoTime();
        try {
            URI uri = URI.create(endpoint);
            if (!isProbeAllowed(uri)) {
                return new ProbeResult(false, 0, "拨测地址不在允许范围内（仅公网 http/https）");
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeout))
                    // 不跟随跳转：否则服务端可用 302 把请求引向内网 / 元数据服务，绕过上面的地址校验
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest head = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(timeout))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = client.send(head, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code == 405 || code == 501 || code >= 400) {
                // 不支持 HEAD 或以 4xx 拒绝：改用 GET 再探一次，避免误判
                HttpRequest get = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofMillis(timeout))
                        .GET()
                        .build();
                response = client.send(get, HttpResponse.BodyHandlers.discarding());
                code = response.statusCode();
            }
            int latency = (int) ((System.nanoTime() - begin) / 1_000_000);
            boolean ok = code >= 200 && code < 400;
            return new ProbeResult(ok, latency, "HTTP " + code);
        } catch (Exception e) {
            int latency = (int) ((System.nanoTime() - begin) / 1_000_000);
            String detail = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().isBlank()) {
                detail += " · " + e.getMessage();
            }
            return new ProbeResult(false, latency, detail.length() > 80 ? detail.substring(0, 80) : detail);
        }
    }

    /**
     * 拨测地址准入校验（SSRF 防护）：仅允许公网 http/https，
     * 拒绝回环、私有网段、链路本地（含云服务元数据 169.254.169.254）地址。
     *
     * <p>注：域名解析与真正发起请求之间存在 TOCTOU 窗口（DNS rebinding）；
     * 若将来开放「用户可自行配置拨测地址」，应改为服务端白名单下发可选服务。
     */
    private static boolean isProbeAllowed(URI uri) {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return !(address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isAnyLocalAddress());
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /** 单次拨测结果。 */
    private record ProbeResult(boolean ok, int latencyMs, String detail) {
    }
}
