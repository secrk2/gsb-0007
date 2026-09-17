package com.tean.maintenance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 维保记录。clientId 为前端生成的幂等键（UUID），
 * 离线恢复合并与重复提交均不会产生重复记录。
 */
@Data
@Entity
@Table(name = "maintenance_record", indexes = {
        @Index(name = "idx_mnt_device", columnList = "deviceId"),
        @Index(name = "uk_mnt_client", columnList = "clientId", unique = true)
})
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 幂等键：同一 clientId 只落库一次 */
    @Column(nullable = false, length = 64)
    private String clientId;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Long maintainerId;

    @Column(nullable = false, length = 64)
    private String maintainerName;

    @Column(nullable = false, length = 512)
    private String content;

    /** 维保实际发生时间（离线时由客户端填报） */
    @Column(nullable = false)
    private LocalDateTime happenedAt;

    /** 是否离线补录 */
    @Column(nullable = false)
    private boolean offline;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
