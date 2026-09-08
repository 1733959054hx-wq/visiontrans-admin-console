package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户经营概览视图对象（模块自有 DTO）。
 *
 * <p>前端一屏所需的全部数据:四张核心指标卡 + 近 7 日趋势 + 订单摘要。
 * 数值以原始类型下发,由前端做千分位与单位格式化(与后台管理 VO 字符串化相反,
 * 本模块统一走「前端格式化」口径,便于后续图表复用数值)。
 *
 * @param kpis   核心指标卡(label / value / unit / delta,可空)
 * @param days   近 7 日日期标签(升序)
 * @param expose 近 7 日曝光量(与 days 对齐)
 * @param consume 近 7 日广告消耗(元,与 days 对齐)
 * @param gmv    近 7 日成交 GMV(元,与 days 对齐)
 * @param orderSummary 订单摘要(总笔数 / 待结算 / 退款中)
 */
public record MerchantOverviewVO(List<Kpi> kpis,
                                 List<String> days,
                                 List<Long> expose,
                                 List<BigDecimal> consume,
                                 List<BigDecimal> gmv,
                                 OrderSummary orderSummary) {

    public record Kpi(String label, String value, String unit, String delta) {
    }

    public record OrderSummary(long total, long pending, long refunding, long settled) {
    }
}
