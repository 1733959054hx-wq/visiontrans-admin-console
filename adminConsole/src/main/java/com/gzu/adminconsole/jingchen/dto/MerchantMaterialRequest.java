package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/**
 * 素材新增 / 编辑请求（jingchen 模块）。
 *
 * <p>真实上传流程：先调 {@code POST /merchant/files}（kind=image/video）取得
 * {@code fileName / fileUrl}，再提交本请求入库；服务端按落盘文件回填真实大小，
 * 未显式指定形态时按扩展名自动识别。
 */
public record MerchantMaterialRequest(String name, String materialType, Long sizeKb, Long exposure,
                                      BigDecimal ctr, String status,
                                      String fileName, String fileUrl) {
}
