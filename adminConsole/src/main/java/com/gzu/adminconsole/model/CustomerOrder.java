package com.gzu.adminconsole.model;

/**
 * C 端用户订单。
 *
 * @param id       主键，新增时为 null
 * @param orderNo  订单号
 * @param customer 客户昵称
 * @param type     订单类型：会员 / 课程 / 数字内容
 * @param amount   金额文案（如 "¥ 128.00"）
 * @param status   状态：待支付 / 已支付 / 支付异常 / 退款中 / 已退款 / 已关闭
 * @param created  下单时间
 * @param note     备注
 */
public record CustomerOrder(Long id,
                            String orderNo,
                            String customer,
                            String type,
                            String amount,
                            String status,
                            String created,
                            String note) {

    /** 状态：待支付。 */
    public static final String STATUS_PENDING = "待支付";
    /** 状态：已支付。 */
    public static final String STATUS_PAID = "已支付";
    /** 状态：支付异常。 */
    public static final String STATUS_ABNORMAL = "支付异常";
    /** 状态：退款中。 */
    public static final String STATUS_REFUNDING = "退款中";
    /** 状态：已退款。 */
    public static final String STATUS_REFUNDED = "已退款";
    /** 状态：已关闭。 */
    public static final String STATUS_CLOSED = "已关闭";
}
