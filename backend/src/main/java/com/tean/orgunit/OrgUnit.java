package com.tean.orgunit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用单位（或检验机构本身）。
 */
@Getter
@Setter
@Entity
@Table(name = "org_unit")
public class OrgUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String region;
}
