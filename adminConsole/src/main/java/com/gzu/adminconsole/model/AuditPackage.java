package com.gzu.adminconsole.model;

/**
 * 语种包 / 课程知识包审核记录。
 *
 * @param id     主键，新增时为 null
 * @param type   包类型：语种包 / 课程知识包
 * @param name   包名称
 * @param source 来源
 * @param meta   附加信息（词条数 / 知识点数等）
 * @param status 状态：待审核 / 已发布 / 已驳回
 */
public record AuditPackage(Long id,
                           String type,
                           String name,
                           String source,
                           String meta,
                           String status) {

    /** 状态：待审核。 */
    public static final String STATUS_PENDING = "待审核";
    /** 状态：已发布。 */
    public static final String STATUS_PUBLISHED = "已发布";
    /** 状态：已驳回。 */
    public static final String STATUS_REJECTED = "已驳回";
}
