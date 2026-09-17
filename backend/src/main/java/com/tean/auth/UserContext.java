package com.tean.auth;

/** 请求级登录用户上下文（ThreadLocal），由 AuthInterceptor 设置与清理 */
public final class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw com.tean.common.BizException.unauthorized("未登录或登录已过期");
        }
        return u;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
