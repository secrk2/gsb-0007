package com.tean.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统账号。机构侧账号（检验员/监察员）orgId 为空，可跨单位查看。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt 密文 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 64)
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    private Long orgId;

    @Column(nullable = false)
    private Boolean enabled = true;
}
