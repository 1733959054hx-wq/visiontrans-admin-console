package com.gzu.adminconsole.dto.meta;

import com.gzu.adminconsole.jingchen.dto.MerchantLoginVO;

/**
 * 统一登录结果：不区分入口，由后端按账号自动识别身份后返回。
 *
 * <p>管理员与商户仍然签发各自体系的令牌（会话表、角色域完全不变），
 * 本 DTO 只负责把两套登录结果归一化，前端凭 {@link #identity} 区分落地页与后续 /me、/logout 走向。
 *
 * @param token    登录令牌（管理员或商户令牌，前端统一放入 X-Auth-Token）
 * @param identity 登录身份：admin / merchant
 * @param profile  归一化后的身份档案
 * @param expireAt 过期时间文案
 */
public record UnifiedLoginVO(String token, String identity, Profile profile, String expireAt) {

    /**
     * 归一化身份档案。
     *
     * @param account  登录账号（管理员为用户名，商户为商户编码）
     * @param name     展示名
     * @param role     角色中文名
     * @param roleCode 角色编码（SUPER_ADMIN / OPERATIONS / AUDITOR / MERCHANT）
     * @param group    所属权限组（商户无此概念，为 null）
     * @param avatar   头像文字
     */
    public record Profile(String account, String name, String role, String roleCode, String group,
                          String avatar) {
    }

    /** 管理员登录结果归一化。 */
    public static UnifiedLoginVO ofAdmin(String username, LoginResultVO result) {
        LoginResultVO.AdminProfile p = result.profile();
        return new UnifiedLoginVO(result.token(), "admin",
                new Profile(username, p.name(), p.role(), p.roleCode(), p.group(), p.avatar()),
                result.expireAt());
    }

    /** 商户登录结果归一化。 */
    public static UnifiedLoginVO ofMerchant(MerchantLoginVO result) {
        MerchantLoginVO.MerchantProfile p = result.profile();
        return new UnifiedLoginVO(result.token(), "merchant",
                new Profile(p.code(), p.name(), p.role(), p.roleCode(), null, p.avatar()),
                result.expireAt());
    }
}
