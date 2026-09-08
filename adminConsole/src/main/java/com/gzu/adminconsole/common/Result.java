package com.gzu.adminconsole.common;

import java.time.Instant;

/**
 * 统一 REST 响应包装体（View 层与前端约定的契约）。
 *
 * @param code     业务状态码
 * @param message  提示信息
 * @param data     业务数据
 * @param timestamp 响应时间戳（毫秒）
 */
public record Result<T>(int code, String message, T data, long timestamp) {

    /** 构造成功响应。 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data,
                Instant.now().toEpochMilli());
    }

    /** 构造无数据的成功响应。 */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 按指定状态码构造失败响应。 */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, Instant.now().toEpochMilli());
    }

    /** 按指定状态码与自定义信息构造失败响应。 */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null, Instant.now().toEpochMilli());
    }

    /** 自定义状态码与信息。 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, Instant.now().toEpochMilli());
    }
}
