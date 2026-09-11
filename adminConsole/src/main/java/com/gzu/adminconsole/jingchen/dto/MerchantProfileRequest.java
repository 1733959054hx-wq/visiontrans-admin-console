package com.gzu.adminconsole.jingchen.dto;

/** 商户资料维护请求（jingchen 模块）。展示名与商户编码由会话决定，不接受前端修改。 */
public record MerchantProfileRequest(String contact, String phone, String settleAccount) {
}
