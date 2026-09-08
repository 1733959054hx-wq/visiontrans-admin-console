package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售报表视图对象（jingchen 模块）。
 *
 * @param kpis   报表指标(销售额 / 佣金支出 / 成交笔数 / 推广渠道数)
 * @param days   近 7 日日期标签
 * @param gmv    近 7 日成交 GMV(与 days 对齐)
 * @param byChannel 各渠道成交汇总(按订单表聚合)
 */
public record MerchantSalesVO(List<Kpi> kpis, List<String> days, List<BigDecimal> gmv, List<ChannelRow> byChannel) {

    public record Kpi(String label, String value, String unit, String delta) {
    }

    public record ChannelRow(String channel, long deals, BigDecimal amount, BigDecimal commission) {
    }
}
