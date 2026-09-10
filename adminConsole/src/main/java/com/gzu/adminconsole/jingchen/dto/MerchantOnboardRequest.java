package com.gzu.adminconsole.jingchen.dto;

/** 商户入驻资质提交请求（jingchen 模块）。 */
public record MerchantOnboardRequest(String merchantName, String licenseNo, String contact,
                                     String phone, String qualification) {
}
