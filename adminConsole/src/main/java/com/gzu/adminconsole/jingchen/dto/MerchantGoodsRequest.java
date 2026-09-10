package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/** 商品新增 / 编辑请求（jingchen 模块）。折扣 30-100(100 = 不打折),折后价由后端计算。 */
public record MerchantGoodsRequest(String goodsName, String goodsType, String description,
                                   BigDecimal price, Integer discount, Long salesCount) {
}
