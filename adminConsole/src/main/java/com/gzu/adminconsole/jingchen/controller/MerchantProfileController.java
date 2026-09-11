package com.gzu.adminconsole.jingchen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantProfileRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantProfileVO;
import com.gzu.adminconsole.jingchen.service.MerchantProfileService;

/**
 * 商户资料接口（jingchen 模块,账户管理-基本信息维护）。
 * 类级 {@code @RequireRole(MERCHANT)}:仅商户令牌可访问。
 * 商户编码一律取自 {@link AdminContext},不信任前端传入。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/profile")
public class MerchantProfileController {

    private final MerchantProfileService service;

    public MerchantProfileController(MerchantProfileService service) {
        this.service = service;
    }

    /** 查看当前商户资料。 */
    @GetMapping
    public Result<com.gzu.adminconsole.jingchen.dto.MerchantProfileVO> get() {
        return Result.ok(service.get(AdminContext.get().username()));
    }

    /** 维护资料(联系人 / 电话 / 结算账户)。 */
    @PutMapping
    public Result<com.gzu.adminconsole.jingchen.dto.MerchantProfileVO> update(@RequestBody MerchantProfileRequest req) {
        return Result.ok(service.update(AdminContext.get().username(), req));
    }
}
