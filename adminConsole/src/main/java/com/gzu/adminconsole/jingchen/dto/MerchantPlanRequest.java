package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;

/**
 * 商户投放计划新增 / 编辑请求（jingchen 模块）。
 *
 * <p>创建与编辑共用一个结构；{@code id}、{@code used}、{@code status} 由后端维护，
 * 前端提交时无需携带（携带也会被忽略）。
 *
 * @param planNo     业务编号(选填,空则自动生成)
 * @param name       计划名称(必填)
 * @param adForm     广告形式
 * @param scene      定向场景
 * @param budget     日预算(元)
 * @param owner      项目负责人(选填)
 * @param ctr        点击率(%,选填)
 * @param slotName   所选广告位名称(投放设置)
 * @param timeRange  投放时段(投放设置)
 * @param targeting  目标人群定向(逗号分隔)
 */
public record MerchantPlanRequest(String planNo, String name, String adForm, String scene,
                                  BigDecimal budget, String owner, BigDecimal ctr,
                                  String slotName, String timeRange, String targeting) {
}
