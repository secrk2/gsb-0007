package com.tean.security;

import com.tean.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析。令牌中携带用户 id、角色与所属单位，服务端无状态鉴权。
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${tean.jwt-secret}") String secret,
                      @Value("${tean.jwt-ttl-hours:12}") long ttlHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlHours * 3600_000L;
    }

    public String generate(AuthUser user) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("role", user.getRole().name())
                .claim("orgId", user.getOrgId())
                .claim("orgName", user.getOrgName())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthUser parse(String token) {
        Claims c = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        Long orgId = c.get("orgId") == null ? null : ((Number) c.get("orgId")).longValue();
        return new AuthUser(
                Long.valueOf(c.getSubject()),
                c.get("username", String.class),
                c.get("realName", String.class),
                Role.valueOf(c.get("role", String.class)),
                orgId,
                c.get("orgName", String.class));
    }
}
