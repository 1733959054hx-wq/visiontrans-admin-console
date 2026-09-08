package com.gzu.adminconsole.jingchen.model;

/**
 * 商户档案领域模型（模块自有，对应 {@code merchant_account} 表）。
 *
 * @param id        主键，新增时为 null
 * @param code      商户编码（与登录账号一致）
 * @param name      商户名称
 * @param contact   联系人
 * @param phone     联系电话（脱敏展示）
 * @param status    状态：启用 / 停用
 * @param createdAt 入驻时间
 */
public record MerchantAccount(Long id,
                              String code,
                              String name,
                              String contact,
                              String phone,
                              String status,
                              String createdAt) {

    /** 构造一条新商户档案（无主键）。 */
    public static MerchantAccount of(String code, String name, String contact, String phone, String status,
                                     String createdAt) {
        return new MerchantAccount(null, code, name, contact, phone, status, createdAt);
    }
}
