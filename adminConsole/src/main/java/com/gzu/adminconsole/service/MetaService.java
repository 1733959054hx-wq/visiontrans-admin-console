package com.gzu.adminconsole.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.dto.meta.NavMenuVO;
import com.gzu.adminconsole.dto.meta.SystemStatusVO;
import com.gzu.adminconsole.model.NavMenu;
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

    /**
     * 侧边导航菜单：按当前登录管理员的角色过滤后，按一级页面分组下发。
     *
     * <p>未登录（公开页调用）时返回空分组，登录后由前端重新拉取。
     */
    public NavMenuVO nav() {
        AdminContext.CurrentAdmin admin = AdminContext.get();
        String roleCode = admin == null ? "" : admin.roleCode();
        // 保序分组：LinkedHashMap 保持菜单在库中的 sortOrder 顺序
        Map<String, List<NavMenu>> grouped = new LinkedHashMap<>();
        Map<String, String> groupSubs = new LinkedHashMap<>();
        for (NavMenu menu : navRepository.findByRole(roleCode)) {
            String group = StringUtils.hasText(menu.groupName()) ? menu.groupName() : NavMenu.DEFAULT_GROUP;
            grouped.computeIfAbsent(group, key -> new ArrayList<>()).add(menu);
            groupSubs.putIfAbsent(group, menu.groupSub() == null ? "" : menu.groupSub());
        }
        List<NavMenuVO.NavGroup> groups = grouped.entrySet().stream()
                .map(e -> new NavMenuVO.NavGroup(e.getKey(), groupSubs.getOrDefault(e.getKey(), ""),
                        e.getValue().stream()
                                .map(m -> new NavMenuVO.NavItem(m.id(), m.icon(), m.text(), m.sub(), m.title(),
                                        m.desc()))
                                .toList()))
                .toList();
        return new NavMenuVO(groups, roleCode);
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
