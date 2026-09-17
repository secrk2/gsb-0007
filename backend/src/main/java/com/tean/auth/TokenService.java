package com.tean.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** 基于 Redis 的令牌服务：登录签发、校验续期、注销 */
@Component
@RequiredArgsConstructor
public class TokenService {
    private static final String KEY_PREFIX = "tean:token:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${tean.token-ttl-hours:12}")
    private long ttlHours;

    public String issue(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        save(token, user);
        return token;
    }

    public Optional<LoginUser> resolve(String token) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + token);
            if (json == null) {
                return Optional.empty();
            }
            // 滑动续期
            redis.expire(KEY_PREFIX + token, Duration.ofHours(ttlHours));
            return Optional.of(objectMapper.readValue(json, LoginUser.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void revoke(String token) {
        redis.delete(KEY_PREFIX + token);
    }

    private void save(String token, LoginUser user) {
        try {
            redis.opsForValue().set(KEY_PREFIX + token,
                    objectMapper.writeValueAsString(user), Duration.ofHours(ttlHours));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签发失败", e);
        }
    }
}
