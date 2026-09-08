package com.gzu.adminconsole.common;

/**
 * 统一响应状态码。
 */
public enum ResultCode {

    /** 成功。 */
    SUCCESS(200, "OK"),
    /** 业务校验失败。 */
    BAD_REQUEST(400, "请求参数不合法"),
    /** 资源不存在。 */
    NOT_FOUND(404, "资源不存在"),
    /** 服务内部错误。 */
    INTERNAL_ERROR(500, "服务内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
