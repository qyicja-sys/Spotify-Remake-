package com.ty1l.spotify_remake.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 *
 * 提供统一的缓存读写能力，支持全局版本号机制。
 *
 * 版本号机制：
 * - 全局版本号 key: spotify:version:{domain}，如 spotify:version:home
 * - 管理员修改共享数据时 INCR 全局版本号
 * - 读取时比对缓存内版本号 vs 全局版本号，不匹配则视为过期
 *
 * Key 命名规范（遵循 CLAUDE.md）：{业务}:{模块}:{key}
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    public static final String KEY_HOME = "spotify:home:%s";           // userId
    public static final String KEY_PROFILE = "spotify:profile:%s";     // userId
    public static final String KEY_ARTIST = "spotify:artist:%s";       // artistId
    public static final String KEY_VERSION_HOME = "spotify:version:home";

    // 默认 TTL（秒）
    private static final long DEFAULT_TTL = 1800; // 30 minutes

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public CacheService(StringRedisTemplate stringRedisTemplate) {
        this.redis = stringRedisTemplate;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // ── 基本操作 ────────────────────────────────────────────────────────

    /** 读取缓存并反序列化 */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isEmpty()) return null;
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Cache get failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    /** 写入缓存 */
    public void set(String key, Object value, long ttlSeconds) {
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache set failed for key={}: {}", key, e.getMessage());
        }
    }

    /** 写入缓存（默认 TTL） */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    /** 删除缓存 */
    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Cache delete failed for key={}: {}", key, e.getMessage());
        }
    }

    /** 批量删除匹配 pattern 的 key（谨慎使用，内部走 SCAN） */
    public void deleteByPattern(String pattern) {
        try {
            var keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("Cache deleted {} keys matching pattern={}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache deleteByPattern failed for pattern={}: {}", pattern, e.getMessage());
        }
    }

    // ── 版本号操作 ──────────────────────────────────────────────────────

    /** 获取全局版本号 */
    public long getVersion(String versionKey) {
        try {
            String v = redis.opsForValue().get(versionKey);
            return v != null ? Long.parseLong(v) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 递增全局版本号（原子操作），返回新版本号 */
    public long incrVersion(String versionKey) {
        try {
            Long v = redis.opsForValue().increment(versionKey);
            log.info("Version incremented: {} -> {}", versionKey, v);
            return v != null ? v : 0L;
        } catch (Exception e) {
            log.warn("Version increment failed for key={}: {}", versionKey, e.getMessage());
            return 0L;
        }
    }

    // ── 带版本的缓存操作 ────────────────────────────────────────────────

    /** 写入带版本号的缓存 */
    public void setWithVersion(String key, Object value, long version, long ttlSeconds) {
        set(key, new VersionedWrapper<>(value, version), ttlSeconds);
    }

    /** 读取带版本号的缓存，版本匹配才返回 */
    public <T> T getWithVersion(String key, Class<T> clazz, long expectedVersion) {
        VersionedWrapper<T> wrapper = get(key, VersionedWrapper.class, clazz);
        if (wrapper == null) return null;
        if (wrapper.version != expectedVersion) {
            log.debug("Cache version mismatch for key={}: cached={}, expected={}", key, wrapper.version, expectedVersion);
            return null;
        }
        return wrapper.data;
    }

    // ── 内部类 ──────────────────────────────────────────────────────────

    /** 带版本号的缓存包装 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionedWrapper<T> {
        private T data;
        private long version;
    }

    // ── 反序列化辅助 ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T get(String key, Class<?> wrapperClass, Class<T> dataClass) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isEmpty()) return null;
            // 解析为包装类型，再提取 data
            var javaType = mapper.getTypeFactory().constructParametricType(
                    (Class<?>) wrapperClass, dataClass);
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("Cache get failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }
}
