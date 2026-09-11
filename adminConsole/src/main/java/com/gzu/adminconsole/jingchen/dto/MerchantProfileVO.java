package com.gzu.adminconsole.jingchen.dto;

/**
 * 商户资料视图对象（jingchen 模块）。
 *
 * @param code          商户编码(= 登录账号)
 * @param merchantName  商户展示名
 * @param contact       联系人
 * @param phone         联系电话
 * @param settleAccount 结算账户摘要
 * @param status        账号状态
 * @param lastLogin     最近登录
 */
public record MerchantProfileVO(String code, String merchantName, String contact, String phone,
                                String settleAccount, String status, String lastLogin) {
}
