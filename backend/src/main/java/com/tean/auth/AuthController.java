package com.tean.auth;

import com.tean.common.ApiResponse;
import com.tean.common.BizException;
import com.tean.user.UserEntity;
import com.tean.user.UserRepository;
import com.tean.org.OrgRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final OrgRepository orgRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        UserEntity user = userRepository.findByUsername(req.username())
                .filter(UserEntity::isEnabled)
                .orElseThrow(() -> BizException.badRequest("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        String orgName = orgRepository.findById(user.getOrgId())
                .map(o -> o.getName()).orElse("未知机构");
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getName(),
                user.getRole(), user.getOrgId(), orgName);
        String token = tokenService.issue(loginUser);
        return ApiResponse.ok(Map.of(
                "token", token,
                "user", loginUser));
    }

    @GetMapping("/me")
    public ApiResponse<LoginUser> me() {
        return ApiResponse.ok(UserContext.require());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = AuthInterceptor.tokenOf(request);
        if (token != null) {
            tokenService.revoke(token);
        }
        return ApiResponse.ok(null);
    }
}
