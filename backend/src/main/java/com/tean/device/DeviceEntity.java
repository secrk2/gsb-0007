package com.tean.device;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "device", indexes = {
        @Index(name = "idx_device_org", columnList = "orgId"),
        @Index(name = "idx_device_maint_org", columnList = "maintOrgId"),
        @Index(name = "idx_device_status", columnList = "status"),
        @Index(name = "uk_device_code", columnList = "code", unique = true)
})
public class DeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备注册代码（唯一） */
    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceStatus status;

    /** 使用单位 */
    @Column(nullable = false)
    private Long orgId;

    /** 维保单位 */
    @Column(nullable = false)
    private Long maintOrgId;

    /** 脱敏位置：区域 + 编号，如「城东片区 · A-101」 */
    @Column(nullable = false, length = 32)
    private String regionCode;

    @Column(nullable = false, length = 32)
    private String locationCode;

    /** 精确位置（敏感）：建筑名称与具体位置，仅监察员二次确认后可见 */
    @Column(nullable = false, length = 128)
    private String buildingName;

    @Column(nullable = false, length = 256)
    private String exactAddress;

    private LocalDate installDate;

    private LocalDate lastInspectionDate;

    private LocalDate nextInspectionDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
