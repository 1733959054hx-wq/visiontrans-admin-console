package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/** 素材新增 / 编辑请求（jingchen 模块）。 */
public record MerchantMaterialRequest(String name, String materialType, Long sizeKb, Long exposure,
                                      BigDecimal ctr, String status) {
}
