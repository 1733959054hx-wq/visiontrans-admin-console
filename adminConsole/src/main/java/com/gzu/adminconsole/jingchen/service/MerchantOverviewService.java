package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.jingchen.dto.MerchantOverviewVO;
import com.gzu.adminconsole.jingchen.entity.MerchantDailyStatEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantOrderRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantStatRepository;

/**
 * 商户经营概览业务逻辑（模块自有）。
 */
@Service
public class MerchantOverviewService {

    private final MerchantStatRepository statRepository;
    private final MerchantOrderRepository orderRepository;

    public MerchantOverviewService(MerchantStatRepository statRepository, MerchantOrderRepository orderRepository) {
        this.statRepository = statRepository;
        this.orderRepository = orderRepository;
    }

    /** 组装经营概览数据:指标卡 + 近 7 日趋势 + 订单摘要。 */
    public MerchantOverviewVO overview() {
        List<MerchantDailyStatEntity> stats = statRepository.findAll();
        List<MerchantDailyStatEntity> recent = stats.size() <= 7 ? stats : stats.subList(stats.size() - 7, stats.size());

        long expo = recent.stream().mapToLong(MerchantDailyStatEntity::getExposure).sum();
        BigDecimal consume = sum(recent, MerchantDailyStatEntity::getConsume);
        BigDecimal gmv = sum(recent, MerchantDailyStatEntity::getGmv);
        long pending = orderRepository.countByStatus("待结算");
        long refunding = orderRepository.countByStatus("退款中");
        long total = orderRepository.count();

        List<MerchantOverviewVO.Kpi> kpis = List.of(
                new MerchantOverviewVO.Kpi("近 7 日曝光量", String.valueOf(expo), "次", null),
                new MerchantOverviewVO.Kpi("近 7 日广告消耗", consume.toPlainString(), "元", null),
                new MerchantOverviewVO.Kpi("近 7 日成交 GMV", gmv.toPlainString(), "元", null),
                new MerchantOverviewVO.Kpi("订单笔数", String.valueOf(total), "笔",
                        "待结算 " + pending + " · 退款中 " + refunding));

        return new MerchantOverviewVO(kpis,
                recent.stream().map(MerchantDailyStatEntity::getStatDate).toList(),
                recent.stream().map(MerchantDailyStatEntity::getExposure).toList(),
                recent.stream().map(MerchantDailyStatEntity::getConsume).toList(),
                recent.stream().map(MerchantDailyStatEntity::getGmv).toList(),
                new MerchantOverviewVO.OrderSummary(total, pending, refunding,
                        total - pending - refunding));
    }

    private static BigDecimal sum(List<MerchantDailyStatEntity> list, java.util.function.Function<MerchantDailyStatEntity, BigDecimal> fn) {
        return list.stream().map(fn).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
