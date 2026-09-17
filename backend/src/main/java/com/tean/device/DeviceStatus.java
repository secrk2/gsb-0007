package com.tean.device;

/** 设备档案状态机：注册告知 → 验收 → 在用 ⇄ 停用 → 报废 */
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

    public String getLabel() {
        return label;
    }
}
