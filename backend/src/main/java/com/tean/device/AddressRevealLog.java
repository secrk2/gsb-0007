package com.tean.device;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 监察员查看精确地址的留痕 */
@Data
@Entity
@Table(name = "address_reveal_log", indexes = @Index(name = "idx_reveal_device", columnList = "deviceId"))
public class AddressRevealLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String userName;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
