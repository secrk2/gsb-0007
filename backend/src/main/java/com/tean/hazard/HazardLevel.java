package com.tean.hazard;

import lombok.Getter;

@Getter
public enum HazardLevel {
    HIGH("重大隐患"),
    MEDIUM("一般隐患"),
    LOW("轻微隐患");

    private final String label;

    HazardLevel(String label) {
        this.label = label;
    }
}
