package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantHomeVO;
import com.gzu.adminconsole.jingchen.service.MerchantService;

/**
 * 商户工作台接口（jingchen 模块）。
 *
 * <p>类级 {@code @RequireRole(MERCHANT)}：仅商户用户可访问，管理员访问同样被拒（403），
 * 与后台管理接口（仅允许管理角色）形成双向隔离。
 *
 * <p>身份一律从 {@link AdminContext} 取当前会话，不信任任何前端传入的账号参数。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX)
public class MerchantController {

    private final MerchantService service;

    public MerchantController(MerchantService service) {
        this.service = service;
    }

    /** 商户工作台首页（占位：仅欢迎信息与档案摘要，业务数据待后端组员补充）。 */
    @GetMapping("/home")
    public Result<MerchantHomeVO> home() {
        AdminContext.CurrentAdmin admin = AdminContext.get();
        if (admin == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return Result.ok(service.home(admin.username(), admin.name(), admin.roleName()));
    }
}
