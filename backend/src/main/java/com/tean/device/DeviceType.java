package com.tean.device;

import lombok.Getter;

@Getter
public enum DeviceType {
    ELEVATOR("电梯"),
    CRANE("起重机械");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }
}
