package com.gzu.adminconsole.model;

/**
 * 管理员操作日志（追加写入 + 哈希链存证，不可篡改）。
 *
 * @param id       主键，新增时为 null
 * @param time     操作时间
 * @param operator 操作人
 * @param role     操作人角色
 * @param group    所属权限组
 * @param action   操作类型
 * @param detail   操作详情
 * @param source   来源 IP / 地域
 * @param result   操作结果：成功 / 已拦截
 * @param hash     存证哈希
 */
public record AuditLogEntry(Long id,
                            String time,
                            String operator,
                            String role,
                            String group,
                            String action,
                            String detail,
                            String source,
                            String result,
                            String hash) {

    /** 构造一条新日志（无主键、无哈希，落库时生成）。 */
    public static AuditLogEntry of(String time, String operator, String role, String group, String action,
                                   String detail, String source, String result) {
        return new AuditLogEntry(null, time, operator, role, group, action, detail, source, result, null);
    }
}
