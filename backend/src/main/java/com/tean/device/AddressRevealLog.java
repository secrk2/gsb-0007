package com.tean.device;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 精确地址查看留痕：谁、何时、因何理由查看了哪台设备的精确地址。
 */
@Getter
@Setter
@Entity
@Table(name = "address_reveal_log")
public class AddressRevealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String realName;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(length = 64)
    private String ip;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
