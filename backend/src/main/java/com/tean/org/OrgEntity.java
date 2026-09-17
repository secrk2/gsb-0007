package com.tean.org;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "org")
public class OrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrgType type;

    /** 行政区划代码（脱敏展示用，如「城东片区」） */
    @Column(length = 32)
    private String regionCode;
}
