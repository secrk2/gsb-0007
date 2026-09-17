package com.tean.common;

import lombok.Getter;

/**
 * 业务异常：code 与前端约定。
 * 401 未登录/令牌失效；403 越权（含跨使用单位访问）；400 业务校验失败（含非法状态流转）。
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException unauthorized(String msg) {
        return new BizException(401, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }
}
