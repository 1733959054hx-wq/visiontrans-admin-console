package com.gzu.adminconsole.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：保证前端始终拿到结构一致的 {@link Result}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常。 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        log.warn("业务异常：{}", ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /** 兜底异常。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("服务内部错误", ex);
        return Result.fail(ResultCode.INTERNAL_ERROR, ex.getMessage());
    }
}
