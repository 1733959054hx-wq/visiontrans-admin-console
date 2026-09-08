package com.gzu.adminconsole.model;

/**
 * 用户退款与争议仲裁工单。
 *
 * @param orderNo       关联订单号
 * @param paid          实付金额文案
 * @param suggestRefund 建议退款金额文案
 * @param advice        仲裁建议
 * @param sla           SLA 剩余时间文案
 * @param reasons       命中规则说明
 * @param status        工单状态
 */
public record RefundRecord(String orderNo,
                           String paid,
                           String suggestRefund,
                           String advice,
                           String sla,
                           String reasons,
                           String status) {
}
