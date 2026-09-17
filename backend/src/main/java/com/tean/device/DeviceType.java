package com.tean.device;

public enum DeviceType {
    ELEVATOR("电梯"),
    CRANE("起重机械");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
