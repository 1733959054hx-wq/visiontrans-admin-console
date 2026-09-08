package com.gzu.adminconsole.jingchen.dto;

import java.util.List;

/**
 * 商户工作台首页视图对象（模块自有 DTO）。
 *
 * <p>当前为占位结构：欢迎信息 + 商户档案摘要 + 说明条目。
 * 后续业务模块直接在本 VO 上扩展字段，不影响前端已接入部分。
 *
 * @param title        页面标题
 * @param merchantName 商户名称
 * @param merchantCode 商户编码
 * @param contact      联系人
 * @param roleName     角色中文名
 * @param welcome      欢迎语
 * @param notices      说明条目
 */
public record MerchantHomeVO(String title,
                             String merchantName,
                             String merchantCode,
                             String contact,
                             String roleName,
                             String welcome,
                             List<String> notices) {
}
