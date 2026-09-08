package com.gzu.adminconsole.jingchen.dto;

/** A/B 测试配置保存请求（jingchen 模块）。 */
public record MerchantAbRequest(String materialA, String materialB, Integer ratioB) {
}
