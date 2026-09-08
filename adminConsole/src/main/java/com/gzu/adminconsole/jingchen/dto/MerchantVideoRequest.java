package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/** 视频档案新增 / 编辑请求（jingchen 模块）。 */
public record MerchantVideoRequest(String name, Long durationSec, BigDecimal sizeGb, String lang,
                                   String status, Long plays, BigDecimal finishRate,
                                   String region, String subtitleLangs) {
}
