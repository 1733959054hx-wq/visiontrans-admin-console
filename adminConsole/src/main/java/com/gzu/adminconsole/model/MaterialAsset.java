package com.gzu.adminconsole.model;

/**
 * AR 广告素材机审记录。
 *
 * @param id          主键，新增时为 null
 * @param name        素材名称
 * @param confidence  机审置信度文案
 * @param verdict     判定结果：通过 / 人工复审 / 驳回
 */
public record MaterialAsset(Long id, String name, String confidence, String verdict) {
}
