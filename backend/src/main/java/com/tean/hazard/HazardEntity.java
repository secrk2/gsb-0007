package com.tean.hazard;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 事故隐患 */
@Data
@Entity
@Table(name = "hazard", indexes = {
        @Index(name = "idx_hazard_device", columnList = "deviceId"),
        @Index(name = "idx_hazard_org", columnList = "orgId")
})
public class HazardEntity {
    public enum Level { GENERAL, MAJOR }
    public enum Status { OPEN, RECTIFIED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 128)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Level level = Level.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.OPEN;

    /** 整改期限，逾期未完成即逾期红点 */
    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime rectifiedAt;

    public boolean isOverdue() {
        return status == Status.OPEN && deadline.isBefore(LocalDate.now());
    }
}
