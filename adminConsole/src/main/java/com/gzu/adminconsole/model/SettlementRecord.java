package com.gzu.adminconsole.model;

/**
 * 商户周期结算记录。
 *
 * @param id         主键，新增时为 null
 * @param merchant   商户名称
 * @param period     结算周期（yyyy-MM）
 * @param orders     周期内订单数
 * @param amount     结算金额文案
 * @param commission 平台佣金文案
 * @param status     状态：待对账 / 已对账 / 已结算
 */
public record SettlementRecord(Long id,
                               String merchant,
                               String period,
                               int orders,
                               String amount,
                               String commission,
                               String status) {

    /** 状态：待对账。 */
    public static final String STATUS_PENDING = "待对账";
    /** 状态：已对账。 */
    public static final String STATUS_RECONCILED = "已对账";
    /** 状态：已结算。 */
    public static final String STATUS_SETTLED = "已结算";
}
