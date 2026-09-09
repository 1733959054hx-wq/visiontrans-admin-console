package com.gzu.adminconsole.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.service.DependencyService;

/**
 * 核心服务与第三方接口定时拨测。
 *
 * <p>默认关闭（避免演示环境无外网时把依赖全部拨成「不可用」）：
 * 需要开启时在 application.properties 中设置 {@code admin-console.dependency.auto-probe=true}，
 * 拨测间隔通过 {@code admin-console.dependency.interval-ms} 调整（默认 10 分钟）。
 * 关闭时仍可在监控运维页手动拨测。
 */
@Component
@ConditionalOnProperty(prefix = "admin-console.dependency", name = "auto-probe", havingValue = "true")
public class DependencyProbeScheduler {

    private static final Logger log = LoggerFactory.getLogger(DependencyProbeScheduler.class);

    private final DependencyService dependencyService;

    public DependencyProbeScheduler(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @Scheduled(initialDelayString = "${admin-console.dependency.initial-delay-ms:30000}",
            fixedDelayString = "${admin-console.dependency.interval-ms:600000}")
    public void probe() {
        log.debug("[依赖拨测] 开始轮询全部核心服务与第三方接口");
        dependencyService.probeAll();
    }
}
