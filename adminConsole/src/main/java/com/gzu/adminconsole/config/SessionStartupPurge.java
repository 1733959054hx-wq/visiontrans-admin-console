package com.gzu.adminconsole.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.repository.AuthRepository;

/**
 * 启动即清理全部登录会话，保证「重启 = 所有人重新登录」。
 *
 * <p>背景：会话持久化在 {@code auth_session} 表，而删除行为只发生在「登出」接口上，
 * 因此单纯重启进程不会让令牌失效 —— 浏览器里残留的旧令牌仍能通过 {@code /auth/me}
 * 校验并直接落到后台首页。这与文档中「后端重启即全部失效」的预期不符。
 *
 * <p>这里在 {@link ApplicationReadyEvent}（上下文与数据源均已就绪）时统一清表；
 * 生产环境如希望重启不踢人，设置 {@code admin-console.security.invalidate-on-startup=false}。
 */
@Component
public class SessionStartupPurge {

    private static final Logger log = LoggerFactory.getLogger(SessionStartupPurge.class);

    private final AuthRepository authRepository;
    private final AppProperties properties;

    public SessionStartupPurge(AuthRepository authRepository, AppProperties properties) {
        this.authRepository = authRepository;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void purgeOnStartup() {
        if (!properties.getSecurity().isInvalidateOnStartup()) {
            log.info("[启动] 已配置保留登录会话（invalidate-on-startup=false），本次不清空");
            return;
        }
        int removed = authRepository.purgeAll();
        log.info("[启动] 已清理历史登录会话 {} 条，所有用户需重新登录", removed);
    }
}
