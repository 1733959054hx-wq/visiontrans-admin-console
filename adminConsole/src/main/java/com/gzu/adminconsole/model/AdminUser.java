package com.gzu.adminconsole.model;

/**
 * 后台管理员账号。
 *
 * @param id         主键，新增时为 null
 * @param name       姓名
 * @param role       角色：超级管理员 / 运营管理员 / 只读审计员
 * @param group      所属权限组
 * @param phone      手机号（脱敏展示用）
 * @param status     状态：启用 / 停用
 * @param lastLogin  最近登录时间
 * @param username   登录账号
 */
public record AdminUser(Long id,
                        String name,
                        String role,
                        String group,
                        String phone,
                        String status,
                        String lastLogin,
                        String username) {
}
