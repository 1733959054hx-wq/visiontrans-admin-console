package com.gzu.adminconsole.dto.security;

import java.util.List;

import com.gzu.adminconsole.dto.security.SecurityOverviewVO.AuditLogRow;

/**
 * 管理员操作日志分页检索结果（监控运维页「日志审计」专用轻量接口）。
 *
 * @param rows       当前页日志行
 * @param page       当前页码（1 基）
 * @param size       每页条数
 * @param total      符合条件的总数
 * @param totalPages 总页数
 * @param rangeText  分页文案
 */
public record AuditLogPageVO(List<AuditLogRow> rows,
                             int page,
                             int size,
                             long total,
                             int totalPages,
                             String rangeText) {
}
