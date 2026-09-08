package com.gzu.adminconsole.model;

/**
 * AI 模型发布版本（端侧量化模型 / 云端大模型统一纳管）。
 *
 * @param name       模型名称（含版本号），业务唯一键
 * @param type       模型类型：端侧 OCR / 端侧翻译 / 云端翻译 ...
 * @param precision  量化精度：INT8 / FP16
 * @param size       包体积文案
 * @param coverage   端侧覆盖率文案
 * @param grayRatio  灰度比例（0-100）
 * @param status     发布状态文案
 */
public record ModelRelease(String name,
                           String type,
                           String precision,
                           String size,
                           String coverage,
                           int grayRatio,
                           String status) {

    /** 复制一份并替换灰度比例。 */
    public ModelRelease withGrayRatio(int ratio) {
        return new ModelRelease(name, type, precision, size, coverage, ratio, status);
    }

    /** 复制一份并替换发布状态。 */
    public ModelRelease withStatus(String newStatus) {
        return new ModelRelease(name, type, precision, size, coverage, grayRatio, newStatus);
    }
}
