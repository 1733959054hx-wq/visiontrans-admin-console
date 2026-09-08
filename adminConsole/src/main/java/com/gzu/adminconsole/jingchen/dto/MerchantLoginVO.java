package com.gzu.adminconsole.jingchen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 商户登录结果（模块自有 DTO，独立于后台管理员 {@code LoginResultVO}）。
 *
 * @param token    商户会话令牌
 * @param profile  商户身份信息
 * @param expireAt 令牌过期时间（展示用字符串）
 */
public record MerchantLoginVO(String token, MerchantProfile profile, String expireAt) {

    /**
     * 商户身份档案（前端适配字段，与后台管理员 profile 字段对齐便于复用 TopBar 等组件）。
     *
     * @param code    商户编码（对应用户名）
     * @param name    展示名
     * @param role    角色中文名
     * @param roleCode 角色编码（MERCHANT）
     * @param avatar  头像字
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MerchantProfile(String code, String name, String role, String roleCode, String avatar) {
    }
}
