package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/**
 * 视频档案新增 / 编辑请求（jingchen 模块）。
 *
 * <p>真实上传流程：先调 {@code POST /merchant/files}（kind=video）取得
 * {@code fileName / fileUrl}，再提交本请求；大小按落盘文件折算 GB，
 * 时长转码后识别，新建时可缺省。
 */
public record MerchantVideoRequest(String name, Long durationSec, BigDecimal sizeGb, String lang,
                                   String status, Long plays, BigDecimal finishRate,
                                   String region, String subtitleLangs,
                                   String fileName, String fileUrl) {
}
