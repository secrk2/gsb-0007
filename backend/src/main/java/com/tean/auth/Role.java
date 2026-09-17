package com.tean.auth;

/**
 * 四类账号角色。
 * DEVICE_ADMIN 设备管理员（使用单位）；MAINTAINER 维保人员（维保单位）；
 * INSPECTOR 检验员（特检机构）；SUPERVISOR 监察员（特检机构，可看精确地址）。
 */
public enum Role {
    DEVICE_ADMIN,
    MAINTAINER,
    INSPECTOR,
    SUPERVISOR;

    public boolean isAgency() {
        return this == INSPECTOR || this == SUPERVISOR;
    }
}
