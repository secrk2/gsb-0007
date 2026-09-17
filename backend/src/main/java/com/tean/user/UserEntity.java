package com.tean.user;

import com.tean.auth.Role;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_user", indexes = @Index(name = "uk_user_username", columnList = "username", unique = true))
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String passwordHash;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 所属机构（使用单位/维保单位/特检机构） */
    @Column(nullable = false)
    private Long orgId;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false)
    private boolean enabled = true;
}
