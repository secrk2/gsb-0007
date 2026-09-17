package com.tean.device;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 设备状态流转留痕。
 */
@Getter
@Setter
@Entity
@Table(name = "device_status_log")
public class DeviceStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(length = 16)
    private String fromStatus;

    @Column(nullable = false, length = 16)
    private String toStatus;

    private Long operatorId;

    @Column(length = 64)
    private String operatorName;

    @Column(length = 255)
    private String reason;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
