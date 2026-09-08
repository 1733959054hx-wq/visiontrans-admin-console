package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/**
 * 商户投放计划新增 / 编辑请求（模块自有 DTO）。
 *
 * <p>创建与编辑共用一个结构；{@code id}、{@code used}、{@code status} 由后端维护，
 * 前端提交时无需携带（携带也会被忽略）。
 *
 * @param planNo 业务编号(选填,空则自动生成)
 * @param name   计划名称(必填)
 * @param adForm 广告形式
 * @param scene  定向场景
 * @param budget 日预算(元)
 * @param owner  项目负责人(选填)
 * @param ctr    点击率(%,选填)
 */
public record MerchantPlanRequest(String planNo, String name, String adForm, String scene,
                                  BigDecimal budget, String owner, BigDecimal ctr) {
}
