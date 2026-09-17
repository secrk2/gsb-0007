package com.tean.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * 作战台汇总缓存（Redis，60s TTL）。Redis 不可用时降级为直查数据库，不影响主流程。
 */
@Slf4j
@Component
public class DashboardCache {

    private static final String KEY_PREFIX = "tean:dash:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    public DashboardCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String get(String scopeKey) {
        try {
            return redis.opsForValue().get(KEY_PREFIX + scopeKey);
        } catch (Exception e) {
            log.warn("Redis 读取失败，降级直查数据库: {}", e.getMessage());
            return null;
        }
    }

    public void put(String scopeKey, String json) {
        try {
            redis.opsForValue().set(KEY_PREFIX + scopeKey, json, TTL);
        } catch (Exception e) {
            log.warn("Redis 写入失败: {}", e.getMessage());
        }
    }

    /** 设备/隐患变更后调用，清除全部作用域缓存 */
    public void evictAll() {
        try {
            Set<String> keys = redis.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 缓存清理失败: {}", e.getMessage());
        }
    }
}
