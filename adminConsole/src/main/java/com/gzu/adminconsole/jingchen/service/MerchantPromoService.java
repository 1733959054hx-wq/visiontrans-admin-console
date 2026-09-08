package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantChannelRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantSalesVO;
import com.gzu.adminconsole.jingchen.entity.MerchantChannelEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantDailyStatEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantOrderEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantChannelRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantOrderRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantStatRepository;

/**
 * 商户推广渠道与销售报表业务逻辑（模块自有）。
 */
@Service
public class MerchantPromoService {

    private final MerchantChannelRepository repository;
    private final MerchantOrderRepository orderRepository;
    private final MerchantStatRepository statRepository;

    public MerchantPromoService(MerchantChannelRepository repository,
                                MerchantOrderRepository orderRepository,
                                MerchantStatRepository statRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.statRepository = statRepository;
    }

    /* ------------------------------ 推广渠道 ------------------------------ */

    public List<MerchantChannelEntity> list() {
        return repository.findAll();
    }

    public MerchantChannelEntity get(Long id) {
        MerchantChannelEntity entity = repository.findById(id);
        if (entity == null) {
            throw new BusinessException(404, "渠道不存在: id=" + id);
        }
        return entity;
    }

    public MerchantChannelEntity create(MerchantChannelRequest req) {
        String code = (req.channelCode() == null || req.channelCode().isBlank())
                ? "CH-" + (System.currentTimeMillis() % 100_000)
                : req.channelCode().trim();
        MerchantChannelEntity entity = new MerchantChannelEntity(
                requireName(req.channelName()), code,
                req.ratioPercent() == null ? 25 : Math.min(50, Math.max(5, req.ratioPercent())),
                req.clickCount() == null ? 0L : req.clickCount(),
                req.dealCount() == null ? 0L : req.dealCount(),
                req.commissionAmount() == null ? BigDecimal.ZERO : req.commissionAmount());
        return repository.save(entity);
    }

    public MerchantChannelEntity update(Long id, MerchantChannelRequest req) {
        MerchantChannelEntity entity = get(id);
        if (req.channelName() != null && !req.channelName().isBlank()) entity.setChannelName(req.channelName().trim());
        if (req.ratioPercent() != null) entity.setRatioPercent(Math.min(50, Math.max(5, req.ratioPercent())));
        if (req.clickCount() != null) entity.setClickCount(req.clickCount());
        if (req.dealCount() != null) entity.setDealCount(req.dealCount());
        if (req.commissionAmount() != null) entity.setCommissionAmount(req.commissionAmount());
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }

    /* ------------------------------ 销售报表 ------------------------------ */

    /** 销售报表:订单聚合出指标与渠道分布,日统计出近 7 日 GMV 趋势。 */
    public MerchantSalesVO sales() {
        List<MerchantOrderEntity> orders = orderRepository.findAll();
        List<MerchantDailyStatEntity> stats = statRepository.findAll();

        long deals = orders.size();
        BigDecimal amount = orders.stream().map(MerchantOrderEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = orders.stream().map(MerchantOrderEntity::getCommission).reduce(BigDecimal.ZERO, BigDecimal::add);
        long channels = repository.count();

        Map<String, long[]> dealsBy = new LinkedHashMap<>();
        Map<String, BigDecimal[]> amountBy = new LinkedHashMap<>();
        for (MerchantOrderEntity o : orders) {
            String ch = o.getChannel() == null || o.getChannel().isBlank() ? "其他" : o.getChannel();
            long[] d = dealsBy.computeIfAbsent(ch, k -> new long[1]);
            d[0]++;
            BigDecimal[] a = amountBy.computeIfAbsent(ch, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(o.getAmount());
            a[1] = a[1].add(o.getCommission());
        }
        List<MerchantSalesVO.ChannelRow> byChannel = dealsBy.keySet().stream().map(ch ->
                new MerchantSalesVO.ChannelRow(ch, dealsBy.get(ch)[0], amountBy.get(ch)[0], amountBy.get(ch)[1])).toList();

        List<MerchantSalesVO.Kpi> kpis = List.of(
                new MerchantSalesVO.Kpi("成交总额", amount.toPlainString(), "元", null),
                new MerchantSalesVO.Kpi("分销佣金支出", commission.toPlainString(), "元", null),
                new MerchantSalesVO.Kpi("成交笔数", String.valueOf(deals), "笔", null),
                new MerchantSalesVO.Kpi("推广渠道", String.valueOf(channels), "个", null));

        List<MerchantDailyStatEntity> recent = stats.size() <= 7 ? stats : stats.subList(stats.size() - 7, stats.size());
        return new MerchantSalesVO(kpis,
                recent.stream().map(MerchantDailyStatEntity::getStatDate).toList(),
                recent.stream().map(MerchantDailyStatEntity::getGmv).toList(),
                byChannel);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "渠道名称必填");
        }
        return name.trim();
    }
}
