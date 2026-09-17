package com.tean.inspection;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 检验记录 */
@Data
@Entity
@Table(name = "inspection_record", indexes = @Index(name = "idx_insp_device", columnList = "deviceId"))
public class InspectionRecord {
    public enum Result { PASS, RECTIFY, FAIL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Long inspectorId;

    @Column(nullable = false, length = 64)
    private String inspectorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Result result;

    @Column(nullable = false)
    private LocalDate inspectedAt;

    /** 下次检验日期，检验完成后回写设备 */
    private LocalDate nextInspectionDate;

    @Column(length = 64)
    private String reportNo;

    @Column(length = 512)
    private String remark;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
