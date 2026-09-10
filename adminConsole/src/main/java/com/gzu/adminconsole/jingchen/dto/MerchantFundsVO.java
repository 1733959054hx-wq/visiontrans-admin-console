package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户资金总览视图对象（jingchen 模块）。
 *
 * @param balance   可用余额(= 累计收入 − 累计支出)
 * @param income    累计收入(元)
 * @param expense   累计支出(元)
 * @param flows     资金流水(最新在前,全量)
 * @param withdraws 提现记录(最新在前,全量)
 */
public record MerchantFundsVO(BigDecimal balance, BigDecimal income, BigDecimal expense,
                              List<FlowItem> flows, List<WithdrawItem> withdraws) {

    public record FlowItem(Long id, String flowType, String direction, BigDecimal amount,
                           BigDecimal balanceAfter, String remark, String createdAt) {
    }

    public record WithdrawItem(Long id, String withdrawNo, BigDecimal amount, String status,
                               String appliedAt, String account) {
    }
}
