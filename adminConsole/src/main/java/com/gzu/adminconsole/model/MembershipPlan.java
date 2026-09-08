package com.gzu.adminconsole.model;

/**
 * 会员套餐与翻译额度配置。
 *
 * @param name        套餐名称
 * @param price       价格文案
 * @param desc        售卖说明
 * @param quota       额度说明
 * @param subscribers 订阅人数文案
 * @param usage       本月额度消耗（百分比字符串，如 "72%"）
 */
public record MembershipPlan(String name,
                             String price,
                             String desc,
                             String quota,
                             String subscribers,
                             String usage) {
}
