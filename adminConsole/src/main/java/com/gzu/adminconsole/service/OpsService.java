package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.dto.cluster.BreakerUpdateRequest;
import com.gzu.adminconsole.dto.cluster.BackupUpdateRequest;
import com.gzu.adminconsole.dto.cluster.OpsPanelVO;
import com.gzu.adminconsole.dto.cluster.SysLogVO;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.entity.BackupPolicyEntity;
import com.gzu.adminconsole.entity.CircuitBreakerEntity;
import com.gzu.adminconsole.entity.SystemLogEntity;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.repository.OpsRepository;
import com.gzu.adminconsole.repository.SecurityRepository;

/**
 * 监控运维 ViewModel 层：系统日志、熔断降级、备份策略与告警阈值。
 */
@Service
public class OpsService {

    /** 熔断器合法状态。 */
    private static final List<String> BREAKER_STATES = List.of("开启", "半开", "关闭", "降级中");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 无法从会话中识别操作人时的占位值。 */
    private static final String UNKNOWN = "未知";
    /** 反向代理透传客户端 IP 的请求头。 */
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final OpsRepository repository;
    private final SecurityRepository securityRepository;

    public OpsService(OpsRepository repository, SecurityRepository securityRepository) {
        this.repository = repository;
        this.securityRepository = securityRepository;
    }

    /** 监控运维面板：熔断降级策略 + 备份策略 + 告警阈值。 */
    public OpsPanelVO opsPanel() {
        List<OpsPanelVO.BreakerRow> breakers = repository.findBreakers().stream()
                .map(b -> new OpsPanelVO.BreakerRow(b.getId(), b.getService(), b.getStrategy(), b.getThreshold(),
                        b.getState(), b.isEnabled()))
                .toList();
        List<OpsPanelVO.BackupRow> backups = repository.findBackups().stream()
                .map(b -> new OpsPanelVO.BackupRow(b.getId(), b.getTarget(), b.getCycle(), b.getRetention(),
                        b.getStorage(), b.isEnabled()))
                .toList();
        OpsPanelVO.Thresholds thresholds = new OpsPanelVO.Thresholds(
                intSetting(OpsRepository.KEY_THRESHOLD_CPU),
                intSetting(OpsRepository.KEY_THRESHOLD_MEM),
                intSetting(OpsRepository.KEY_THRESHOLD_GPU));
        return new OpsPanelVO(breakers, backups, thresholds);
    }

    /** 系统日志（level / category 可空表示不过滤）。 */
    public SysLogVO sysLogs(String level, String category) {
        List<SysLogVO.SysLogRow> rows = repository.findSysLogs(level, category).stream()
                .map(this::toLogRow)
                .toList();
        return new SysLogVO(rows);
    }

    /** 更新熔断降级策略（状态 + 启用开关）。 */
    public ActionResultVO updateBreaker(BreakerUpdateRequest request) {
        if (request == null || request.id() == null) {
            throw new BusinessException("缺少熔断策略主键，无法更新");
        }
        CircuitBreakerEntity breaker = repository.findBreaker(request.id());
        if (breaker == null) {
            throw new BusinessException("未找到熔断策略 #" + request.id());
        }
        if (request.state() == null || !BREAKER_STATES.contains(request.state())) {
            throw new BusinessException("非法的熔断状态：" + request.state() + "，可选值 " + BREAKER_STATES);
        }
        breaker.setState(request.state());
        breaker.setEnabled(request.enabled());
        repository.updateBreaker(breaker);
        writeLog("熔断降级编排", "熔断策略「" + breaker.getService() + "」状态置为 " + request.state()
                + " · " + (request.enabled() ? "已启用" : "已停用"));
        return ActionResultVO.ok("熔断策略「" + breaker.getService() + "」已更新", breaker.getService());
    }

    /** 启停备份策略。 */
    public ActionResultVO updateBackup(BackupUpdateRequest request) {
        if (request == null || request.id() == null) {
            throw new BusinessException("缺少备份策略主键，无法更新");
        }
        BackupPolicyEntity backup = repository.findBackup(request.id());
        if (backup == null) {
            throw new BusinessException("未找到备份策略 #" + request.id());
        }
        backup.setEnabled(request.enabled());
        repository.updateBackup(backup);
        writeLog("备份策略配置", "备份策略「" + backup.getTarget() + "」已"
                + (request.enabled() ? "启用" : "停用"));
        return ActionResultVO.ok("备份策略「" + backup.getTarget() + "」已"
                + (request.enabled() ? "启用" : "停用"), backup.getTarget());
    }

    /** 更新告警阈值（CPU / 内存 / GPU 利用率 %，取值 1 ~ 100）。 */
    public ActionResultVO updateThresholds(int cpu, int mem, int gpu) {
        requireThreshold("cpu", cpu);
        requireThreshold("mem", mem);
        requireThreshold("gpu", gpu);
        repository.setSetting(OpsRepository.KEY_THRESHOLD_CPU, String.valueOf(cpu));
        repository.setSetting(OpsRepository.KEY_THRESHOLD_MEM, String.valueOf(mem));
        repository.setSetting(OpsRepository.KEY_THRESHOLD_GPU, String.valueOf(gpu));
        writeLog("告警阈值配置", "告警阈值调整为 CPU " + cpu + "% · 内存 " + mem + "% · GPU " + gpu + "%");
        return ActionResultVO.ok("告警阈值已更新：CPU " + cpu + "% · 内存 " + mem + "% · GPU " + gpu + "%",
                "ops.threshold");
    }

    /** 阈值取值校验。 */
    private void requireThreshold(String name, int value) {
        if (value < 1 || value > 100) {
            throw new BusinessException("阈值 " + name + " 超出范围（1 ~ 100）：" + value);
        }
    }

    /** 读取阈值设置项，缺省回退默认值。 */
    private int intSetting(String key) {
        String value = repository.getSetting(key);
        if (value == null || value.isBlank()) {
            return switch (key) {
                case OpsRepository.KEY_THRESHOLD_CPU -> 85;
                case OpsRepository.KEY_THRESHOLD_MEM -> 80;
                default -> 90;
            };
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 日志级别配映射：ERROR red、WARN amber、INFO green。 */
    private String levelTone(String level) {
        return switch (level) {
            case "ERROR" -> "red";
            case "WARN" -> "amber";
            default -> "green";
        };
    }

    private SysLogVO.SysLogRow toLogRow(SystemLogEntity log) {
        return new SysLogVO.SysLogRow(log.getId(), log.getTime(), log.getLevel(), levelTone(log.getLevel()),
                log.getCategory(), log.getSource(), log.getMessage());
    }

    private void writeLog(String action, String detail) {
        AdminContext.CurrentAdmin admin = AdminContext.get();
        String name = admin == null ? UNKNOWN : admin.name();
        String role = admin == null ? UNKNOWN : admin.roleName();
        String group = admin == null ? UNKNOWN : admin.groupName();
        AuditLogEntry entry = AuditLogEntry.of(now(), name, role, group, action, detail, clientIp(), "成功");
        securityRepository.pushAuditLog(entry);
    }

    /** 真实来源 IP：优先取反向代理透传的 X-Forwarded-For 首段。 */
    private String clientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return UNKNOWN;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwarded.split(",")[0].trim();
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
