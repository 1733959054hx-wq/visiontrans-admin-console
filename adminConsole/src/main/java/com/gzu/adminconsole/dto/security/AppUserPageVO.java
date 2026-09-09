package com.gzu.adminconsole.dto.security;

import java.util.List;

/**
 * C 端用户分页查询结果（支持关键字与注册方式 / 会员状态 / 账号状态筛选）。
 *
 * @param rows        当前页用户行
 * @param page        当前页码（1 基）
 * @param size        每页条数
 * @param total       符合条件的总数
 * @param totalPages  总页数
 * @param rangeText   分页文案
 * @param regSources  可选注册方式（去重升序，供前端下拉）
 * @param memberships 可选会员状态
 * @param statuses    可选账号状态
 */
public record AppUserPageVO(List<AppUserRow> rows,
                            int page,
                            int size,
                            long total,
                            int totalPages,
                            String rangeText,
                            List<String> regSources,
                            List<String> memberships,
                            List<String> statuses) {

    /** C 端用户账号行。 */
    public record AppUserRow(Long id, String account, String regSource, String membership,
                             String registered, String lastActive, String status) {
    }
}
