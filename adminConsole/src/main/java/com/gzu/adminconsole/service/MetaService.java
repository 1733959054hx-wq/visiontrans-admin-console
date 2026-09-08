package com.gzu.adminconsole.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.dto.meta.NavMenuVO;
import com.gzu.adminconsole.dto.meta.SystemStatusVO;
import com.gzu.adminconsole.repository.NavRepository;

/**
 * 平台元信息 ViewModel 层：导航菜单与系统展示信息。
 */
@Service
public class MetaService {

    private final NavRepository navRepository;
    private final AppProperties properties;

    public MetaService(NavRepository navRepository, AppProperties properties) {
        this.navRepository = navRepository;
        this.properties = properties;
    }

    /** 侧边导航菜单。 */
    public NavMenuVO nav() {
        List<NavMenuVO.NavItem> items = navRepository.findAll().stream()
                .map(m -> new NavMenuVO.NavItem(m.id(), m.icon(), m.text(), m.sub(), m.title(), m.desc()))
                .toList();
        return new NavMenuVO(NavRepository.GROUP, items);
    }

    /** 系统展示信息。 */
    public SystemStatusVO system() {
        AppProperties.Platform p = properties.getPlatform();
        return new SystemStatusVO(p.getName(), p.getSubtitle(), p.getConsoleLabel(), p.getGroupName(),
                p.getClusterStatus(), p.getClusterLatency(), currentDateRange(p.getDateRange()), p.getCurrentUser(),
                p.getCurrentUserId(), p.getCurrentUserAvatar());
    }

    /** 日期范围：未配置时自动按当前真实日期显示「本月 1 号 ~ 月末」。 */
    private static String currentDateRange(String configured) {
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        LocalDate now = LocalDate.now();
        LocalDate first = now.withDayOfMonth(1);
        LocalDate last = now.withDayOfMonth(now.lengthOfMonth());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return fmt.format(first) + " ~ " + fmt.format(last);
    }
}
