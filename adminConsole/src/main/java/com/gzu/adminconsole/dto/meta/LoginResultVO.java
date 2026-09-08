package com.gzu.adminconsole.dto.meta;

/**
 * 登录结果。
 *
 * @param token    登录令牌，前端后续请求放入 X-Auth-Token 请求头
 * @param profile  管理员档案
 * @param expireAt 过期时间文案
 */
public record LoginResultVO(String token, AdminProfile profile, String expireAt) {

    /**
     * @param id       管理员主键，登录态可为 null
     * @param name     姓名
     * @param role     角色中文名
     * @param roleCode 角色编码（SUPER_ADMIN / OPERATIONS / AUDITOR）
     * @param group    所属权限组
     * @param avatar   头像文字（姓名首字）
     */
    public record AdminProfile(Long id, String name, String role, String roleCode, String group,
                               String avatar) {
    }
}
