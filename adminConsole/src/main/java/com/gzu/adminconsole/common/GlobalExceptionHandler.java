package com.gzu.adminconsole.common;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：保证前端始终拿到结构一致的 {@link Result}，同时让 HTTP 状态码与业务码一致。
 *
 * <p>内部异常只返回通用文案 + 追踪号，SQL / 堆栈 / 类路径等细节一律只进日志，不外泄给调用方。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：使用异常自带的业务码作为 HTTP 状态码（401 / 403 / 400 …）。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        log.warn("业务异常：{}", ex.getMessage());
        return ResponseEntity.status(toStatus(ex.getCode()))
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    /** 兜底异常：对外只给通用文案 + 追踪号，细节只进日志。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.error("服务内部错误 traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR, "服务内部错误，请稍后重试（追踪号 " + traceId + "）"));
    }

    /** 业务码 → HTTP 状态码；未知业务码按 400 处理。 */
    private static HttpStatus toStatus(int code) {
        return switch (code) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
