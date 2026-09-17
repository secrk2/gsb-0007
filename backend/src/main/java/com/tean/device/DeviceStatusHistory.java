package com.tean.device;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 设备状态流转留痕 */
@Data
@Entity
@Table(name = "device_status_history", indexes = @Index(name = "idx_history_device", columnList = "deviceId"))
public class DeviceStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceStatus toStatus;

    @Column(length = 256)
    private String reason;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false, length = 64)
    private String operatorName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
