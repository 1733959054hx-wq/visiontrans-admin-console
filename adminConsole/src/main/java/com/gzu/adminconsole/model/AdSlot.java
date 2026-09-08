package com.gzu.adminconsole.model;

/**
 * 广告位库存实体。
 *
 * @param id     主键，新增时为 null
 * @param name   广告位名称
 * @param status 库存状态：已售罄 / 部分售出 / 预售锁定 / 空闲可购
 * @param color  状态色
 * @param remain 剩余库存文案（"预售" 表示锁定中）
 * @param ratio  占用比例，-1 表示预售锁定（无比例）
 */
public record AdSlot(Long id, String name, String status, String color, String remain, double ratio) {
}
