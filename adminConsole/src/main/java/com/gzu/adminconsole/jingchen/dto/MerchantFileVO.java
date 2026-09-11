package com.gzu.adminconsole.jingchen.dto;

/**
 * 上传结果（jingchen 模块）。
 *
 * @param url       文件访问地址（带鉴权，前端需携带 X-Auth-Token 以 blob 方式取回）
 * @param name      原始文件名（如 营业执照.pdf）
 * @param sizeBytes 文件字节数
 * @param sizeKb    文件大小（KB，向上取整，便于入库展示）
 * @param ext       小写扩展名（不含点）
 */
public record MerchantFileVO(String url, String name, long sizeBytes, long sizeKb, String ext) {
}
