package com.tean.device;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 特种设备档案。精确地址（exactAddress）仅入库，默认对外输出「区域 + 编号」脱敏位置。
 */
@Getter
@Setter
@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备注册代码 */
    @Column(nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceType type;

    @Column(length = 128)
    private String model;

    @Column(nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceStatus status;

    /** 所在区域（脱敏位置的一部分） */
    @Column(length = 64)
    private String region;

    /** 脱敏编号，如 A-101 */
    @Column(length = 32)
    private String maskCode;

    /** 精确地址：仅监察员二次确认并留痕后可见 */
    private String exactAddress;

    private LocalDate installDate;

    private LocalDate nextInspectionDate;

    @Version
    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
