package com.tean.security;

import com.tean.user.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录用户上下文（JWT 解析结果），作为 SecurityContext 的 principal。
 */
@Getter
@AllArgsConstructor
public class AuthUser {

    private final Long id;
    private final String username;
    private final String realName;
    private final Role role;
    /** 所属使用单位 id；机构侧账号（检验员/监察员）为 null */
    private final Long orgId;
    private final String orgName;

    /** 使用单位侧账号需要按单位隔离数据 */
    public boolean isOrgScoped() {
        return role == Role.DEVICE_ADMIN || role == Role.MAINTAINER;
    }

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }
}
