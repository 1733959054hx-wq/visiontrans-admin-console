package com.gzu.adminconsole.jingchen.dto;

/** 字幕新增 / 编辑请求（jingchen 模块）：先传文件再提交，lang 必填。 */
public record MerchantSubtitleRequest(String lang, String fileName, String fileUrl) {
}
