package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/** 充值 / 提现请求（jingchen 模块）。 */
public record MerchantFundAmountRequest(BigDecimal amount) {
}
