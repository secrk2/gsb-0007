package com.tean.hazard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备隐患：未闭环且整改期限已过 → 作战台红点。
 */
@Getter
@Setter
@Entity
@Table(name = "hazard")
public class Hazard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HazardLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HazardStatus status = HazardStatus.OPEN;

    /** 整改期限 */
    private LocalDate deadline;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
