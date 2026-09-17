package com.tean.maintenance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 维保记录。recordNo 为客户端生成的幂等键（UUID），唯一约束保证断网重传不产生重复记录。
 */
@Getter
@Setter
@Entity
@Table(name = "maintenance_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_maintenance_record_no", columnNames = "record_no")
})
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 客户端幂等键：同一记录断网重传 / 重复点击只入库一次 */
    @Column(nullable = false, length = 64)
    private String recordNo;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long maintainerId;

    @Column(length = 64)
    private String maintainerName;

    @Column(nullable = false, length = 500)
    private String content;

    /** 离线端原始记录时间 */
    private LocalDateTime clientCreatedAt;

    /** 是否离线补录 */
    @Column(nullable = false)
    private Boolean offline = false;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
