package com.gzu.adminconsole.dto.meta;

/**
 * 变更类操作的统一返回（供前端提示与刷新判断）。
 *
 * @param success 是否成功
 * @param message 提示文案
 * @param related 关联的实体标识（模型名、设备指纹等），可为 null
 */
public record ActionResultVO(boolean success, String message, String related) {

    /** 成功结果。 */
    public static ActionResultVO ok(String message) {
        return new ActionResultVO(true, message, null);
    }

    /** 成功结果（带关联实体）。 */
    public static ActionResultVO ok(String message, String related) {
        return new ActionResultVO(true, message, related);
    }
}
