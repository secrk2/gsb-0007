package com.tean.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 登录后放入 Redis 的会话信息 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {
    private Long id;
    private String username;
    private String name;
    private Role role;
    private Long orgId;
    private String orgName;
}
