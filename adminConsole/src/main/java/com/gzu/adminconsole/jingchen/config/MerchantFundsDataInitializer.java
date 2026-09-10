package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantFundFlowEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantWithdrawEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantFundsRepository;

/**
 * 商户资金演示数据初始化（jingchen 模块）。
 *
 * <p>表空时按业务时间线灌入 9 笔流水(充值/佣金入账/广告消耗/提现/退款,
 * balance_after 逐笔推算保持连贯) + 2 笔提现记录;已有数据一律不覆盖。
 */
@Component
@Order(230)
public class MerchantFundsDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantFundsDataInitializer.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantFundsRepository repository;

    public MerchantFundsDataInitializer(MerchantFundsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.countFlows() > 0 || repository.countWithdraws() > 0) {
            return;
        }
        LocalDateTime base = LocalDateTime.now().minusDays(3);
        BigDecimal balance = BigDecimal.ZERO;
        List<MerchantFundFlowEntity> flows = new ArrayList<>();

        flows.add(flow("充值", "收入", "5000000.00", balance = balance.add(new BigDecimal("5000000.00")), "对公转账充值", base.plusHours(2)));
        flows.add(flow("佣金入账", "收入", "145175.79", balance = balance.add(new BigDecimal("145175.79")), "视频版权分账 ROYALTY-20260831", base.plusHours(20)));
        flows.add(flow("广告消耗", "支出", "248600.00", balance = balance.subtract(new BigDecimal("248600.00")), "广告消耗扣款 PLAN-2026-1031", base.plusHours(30)));
        flows.add(flow("佣金入账", "收入", "33309.48", balance = balance.add(new BigDecimal("33309.48")), "知识包分销 SETTLE-20260902", base.plusHours(44)));
        flows.add(flow("提现", "支出", "500000.00", balance = balance.subtract(new BigDecimal("500000.00")), "提现申请冻结 WD-20260902-3", base.plusHours(52)));
        flows.add(flow("退款", "收入", "6880.00", balance = balance.add(new BigDecimal("6880.00")), "退款退回 REFUND-55210", base.plusHours(60)));
        flows.add(flow("广告消耗", "支出", "612480.60", balance = balance.subtract(new BigDecimal("612480.60")), "广告消耗扣款 PLAN-2026-1028", base.plusHours(70)));
        flows.add(flow("充值", "收入", "300000.00", balance = balance.add(new BigDecimal("300000.00")), "支付宝充值 ALIPAY-9912044", base.plusHours(80)));
        flows.add(flow("广告消耗", "支出", "118800.00", balance = balance.subtract(new BigDecimal("118800.00")), "广告消耗扣款 PLAN-2026-1044", base.plusHours(90)));
        for (MerchantFundFlowEntity f : flows) {
            repository.saveFlow(f);
        }

        repository.saveWithdraw(new MerchantWithdrawEntity("WD-20260902-3", new BigDecimal("500000.00"), "审核中",
                base.plusHours(52).format(FMT), "工行(****8821)"));
        repository.saveWithdraw(new MerchantWithdrawEntity("WD-20260828-1", new BigDecimal("300000.00"), "已到账",
                base.minusDays(6).format(FMT), "工行(****8821)"));
        log.info("[商户端初始化] merchant_fund_flow 已灌入 9 笔流水 / merchant_withdraw 2 笔提现");
    }

    private MerchantFundFlowEntity flow(String type, String direction, String amount,
                                        BigDecimal balanceAfter, String remark, LocalDateTime at) {
        return new MerchantFundFlowEntity(type, direction, new BigDecimal(amount), balanceAfter, remark, at.format(FMT));
    }
}
