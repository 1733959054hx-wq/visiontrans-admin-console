package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.dto.meta.NavMenuVO;
import com.gzu.adminconsole.dto.meta.SystemStatusVO;
import com.gzu.adminconsole.service.MetaService;

/**
 * 平台元信息接口（View 层）：导航菜单与系统展示信息。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/meta")
public class MetaController {

    private final MetaService service;

    public MetaController(MetaService service) {
        this.service = service;
    }

    /** 侧边导航菜单。 */
    @GetMapping("/nav")
    public Result<NavMenuVO> nav() {
        return Result.ok(service.nav());
    }

    /** 系统展示信息。 */
    @GetMapping("/system")
    public Result<SystemStatusVO> system() {
        return Result.ok(service.system());
    }
}
