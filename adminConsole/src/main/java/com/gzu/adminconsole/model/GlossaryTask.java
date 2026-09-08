package com.gzu.adminconsole.model;

/**
 * 多语种术语包审核任务（看板卡片）。
 *
 * @param id        主键，新增时为 null
 * @param title     术语包名称
 * @param priority  优先级：高 / 中 / 低
 * @param meta      附加信息
 * @param owner     负责人
 * @param due       截止时间
 * @param column    所属看板列：待初审 / 术语复核 / 已发布
 */
public record GlossaryTask(Long id,
                           String title,
                           String priority,
                           String meta,
                           String owner,
                           String due,
                           String column) {
}
