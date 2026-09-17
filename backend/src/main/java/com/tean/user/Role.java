package com.tean.user;

import lombok.Getter;

/**
 * 系统角色：设备管理员 / 维保人员（使用单位侧，按单位隔离）；检验员 / 监察员（机构侧，跨单位）。
 */
@Getter
public enum Role {
    DEVICE_ADMIN("设备管理员"),
    MAINTAINER("维保人员"),
    INSPECTOR("检验员"),
    SUPERVISOR("监察员");

    private final String label;

    Role(String label) {
        this.label = label;
    }
}
