package com.tean.device;

import lombok.Getter;

/**
 * 设备档案状态：注册告知 → 验收 → 在用 → 停用 → 报废。
 */
@Getter
public enum DeviceStatus {
    REGISTERED("注册告知"),
    ACCEPTED("验收"),
    IN_USE("在用"),
    SUSPENDED("停用"),
    SCRAPPED("报废");

    private final String label;

    DeviceStatus(String label) {
        this.label = label;
    }
}
