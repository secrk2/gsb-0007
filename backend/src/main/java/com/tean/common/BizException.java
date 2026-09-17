package com.tean.common;

import lombok.Getter;

/**
 * 业务异常：携带 HTTP 状态码与业务错误码，由全局异常处理器统一输出。
 */
@Getter
public class BizException extends RuntimeException {

    private final int status;
    private final String code;

    public BizException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BizException notFound(String message) {
        return new BizException(404, "NOT_FOUND", message);
    }

    public static BizException forbidden(String message) {
        return new BizException(403, "FORBIDDEN", message);
    }

    public static BizException badRequest(String message) {
        return new BizException(400, "BAD_REQUEST", message);
    }

    public static BizException conflict(String code, String message) {
        return new BizException(409, code, message);
    }
}
