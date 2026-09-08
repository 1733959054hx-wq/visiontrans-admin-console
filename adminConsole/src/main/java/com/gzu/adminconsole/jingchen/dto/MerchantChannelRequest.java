package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/** 推广渠道新增 / 编辑请求（jingchen 模块）。 */
public record MerchantChannelRequest(String channelName, String channelCode, Integer ratioPercent,
                                     Long clickCount, Long dealCount, BigDecimal commissionAmount) {
}
