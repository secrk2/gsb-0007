package com.tean.hazard;

import lombok.Getter;

@Getter
public enum HazardStatus {
    OPEN("未闭环"),
    RESOLVED("已闭环");

    private final String label;

    HazardStatus(String label) {
        this.label = label;
    }
}
