package com.tean.auth;

import com.tean.common.ApiResponse;
import com.tean.common.BizException;
import com.tean.orgunit.OrgUnit;
import com.tean.orgunit.OrgUnitRepository;
import com.tean.security.AuthUser;
import com.tean.security.JwtService;
import com.tean.security.SecurityUtils;
import com.tean.user.SysUser;
import com.tean.user.SysUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 登录与当前用户。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(SysUserRepository userRepository, OrgUnitRepository orgUnitRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(@NotBlank(message = "用户名不能为空") String username,
                               @NotBlank(message = "密码不能为空") String password) {
    }

    public record UserInfo(Long id, String username, String realName,
                           String role, String roleLabel, Long orgId, String orgName) {

        static UserInfo of(AuthUser u) {
            return new UserInfo(u.getId(), u.getUsername(), u.getRealName(),
                    u.getRole().name(), u.getRole().getLabel(), u.getOrgId(), u.getOrgName());
        }
    }

    public record LoginResponse(String token, UserInfo user) {
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BizException(401, "BAD_CREDENTIALS", "用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException(401, "DISABLED", "账号已停用，请联系管理员");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(401, "BAD_CREDENTIALS", "用户名或密码错误");
        }
        AuthUser authUser = toAuthUser(user);
        return ApiResponse.ok(new LoginResponse(jwtService.generate(authUser), UserInfo.of(authUser)));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> me() {
        return ApiResponse.ok(UserInfo.of(SecurityUtils.currentUser()));
    }

    private AuthUser toAuthUser(SysUser user) {
        String orgName = null;
        if (user.getOrgId() != null) {
            orgName = orgUnitRepository.findById(user.getOrgId()).map(OrgUnit::getName).orElse(null);
        }
        return new AuthUser(user.getId(), user.getUsername(), user.getRealName(),
                user.getRole(), user.getOrgId(), orgName);
    }
}
