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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Redis 缓存服务
 *
 * 提供统一的缓存读写能力，支持 GZIP 压缩和全局版本号机制。
 *
 * 压缩策略：
 * - JSON 字符串 > 512 字节时自动 GZIP 压缩 + Base64 编码
 * - 压缩后前缀 "GZ:" 标识，读取时自动解压
 * - 无前缀的数据视为未压缩（兼容旧缓存），正常读取
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
    public static final String KEY_SONG = "spotify:song:%s";           // songId
    public static final String KEY_ALBUM = "spotify:album:%s";         // albumId
    public static final String KEY_ALBUM_BY_ARTIST = "spotify:album:artist:%s"; // artistId
    public static final String KEY_LYRICS_LOCAL = "spotify:lyrics:local:%s";    // title
    public static final String KEY_LYRICS_EXTERNAL = "spotify:lyrics:external:%s:%s"; // source, lyricId
    public static final String KEY_EXTERNAL_SEARCH = "spotify:external:search:%s";       // keyword
    public static final String KEY_EXTERNAL_ARTIST = "spotify:external:artist:%s";       // artistName
    public static final String KEY_EXTERNAL_STREAM = "spotify:external:stream:%s:%s";    // source, trackId
    // 播放历史 ZSET（score=播放时间戳 epoch millis, member=songId/artistId）
    public static final String KEY_PLAYBACK_SONGS = "spotify:playback:songs:%s";         // userId
    public static final String KEY_PLAYBACK_ARTISTS = "spotify:playback:artists:%s";     // userId
    public static final String KEY_LEADERBOARD_ZSET = "spotify:leaderboard:global";      // ZSET — member=songId, score=playCount
    public static final String KEY_LEADERBOARD_CACHE = "spotify:leaderboard:global:cache"; // String — 逻辑过期缓存
    public static final String KEY_VERSION_HOME = "spotify:version:home";

    // 默认 TTL（秒）
    private static final long DEFAULT_TTL = 1800; // 30 minutes
    // 歌词 TTL 较长（歌词很少变动）
    public static final long LYRICS_TTL = 7200; // 2 hours
    // 外部 API 搜索 TTL（搜索结果变化较慢，避免频繁调用外部 API）
    public static final long EXTERNAL_SEARCH_TTL = 600; // 10 minutes
    // 外部 API 流媒体 URL TTL（签名 URL 有时效性）
    public static final long EXTERNAL_STREAM_TTL = 300; // 5 minutes

    // GZIP 压缩阈值（字节），小于此值的 JSON 不压缩（压缩收益不大反而增加 CPU 开销）
    private static final int COMPRESS_THRESHOLD = 512;
    // 压缩数据前缀标识（兼容旧缓存：无此前缀的视为未压缩 JSON）
    private static final String GZ_PREFIX = "GZ:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final L1CacheService l1Cache;
    private final CacheLockHelper lockHelper;

    public CacheService(StringRedisTemplate stringRedisTemplate,
                        L1CacheService l1Cache,
                        CacheLockHelper lockHelper) {
        this.redis = stringRedisTemplate;
        this.l1Cache = l1Cache;
        this.lockHelper = lockHelper;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // ── 压缩 / 解压工具方法 ────────────────────────────────────────────────

    /** 将 JSON 字符串压缩为 "GZ:" + Base64(GZIP(bytes)) */
    private String compress(String json) {
        try {
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            if (jsonBytes.length < COMPRESS_THRESHOLD) {
                return json; // 太小不压缩
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
                gzos.write(jsonBytes);
            }
            String encoded = Base64.getEncoder().encodeToString(bos.toByteArray());
            int saved = jsonBytes.length - encoded.length();
            if (saved > 0) {
                log.debug("Cache compress: {} bytes → {} bytes (saved {}%)",
                        jsonBytes.length, encoded.length(), saved * 100 / jsonBytes.length);
            }
            return GZ_PREFIX + encoded;
        } catch (Exception e) {
            log.warn("Cache compress failed, storing uncompressed: {}", e.getMessage());
            return json;
        }
    }

    /** 解压：识别 "GZ:" 前缀自动解压，无前缀按原样返回 */
    private String decompress(String stored) {
        if (stored == null || stored.isEmpty()) return stored;
        if (!stored.startsWith(GZ_PREFIX)) {
            return stored; // 旧缓存，未压缩
        }
        try {
            String encoded = stored.substring(GZ_PREFIX.length());
            byte[] compressed = Base64.getDecoder().decode(encoded);
            ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
            StringBuilder sb = new StringBuilder();
            try (GZIPInputStream gzis = new GZIPInputStream(bis)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = gzis.read(buf)) != -1) {
                    sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Cache decompress failed, returning raw: {}", e.getMessage());
            return stored;
        }
    }

    // ── 基本操作 ────────────────────────────────────────────────────────

    /** 读取缓存并反序列化 */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String stored = redis.opsForValue().get(key);
            if (stored == null || stored.isEmpty()) return null;
            String json = decompress(stored);
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Cache get failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    /** 写入缓存（带随机 TTL 防雪崩，±15% 抖动） */
    public void set(String key, Object value, long ttlSeconds) {
        try {
            String json = mapper.writeValueAsString(value);
            String stored = compress(json);
            redis.opsForValue().set(key, stored, jitter(ttlSeconds), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache set failed for key={}: {}", key, e.getMessage());
        }
    }

    /** 写入缓存（默认 TTL） */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    /** 写入缓存（永久，无物理 TTL）— 用于逻辑过期模式 */
    public void setPermanent(String key, Object value) {
        try {
            String json = mapper.writeValueAsString(value);
            String stored = compress(json);
            redis.opsForValue().set(key, stored);
        } catch (Exception e) {
            log.warn("Cache setPermanent failed for key={}: {}", key, e.getMessage());
        }
    }

    /** 读取缓存并反序列化为 List（解决泛型擦除问题） */
    public <T> List<T> getList(String key, Class<T> elementClass) {
        try {
            String stored = redis.opsForValue().get(key);
            if (stored == null || stored.isEmpty()) return null;
            String json = decompress(stored);
            var javaType = mapper.getTypeFactory().constructCollectionType(List.class, elementClass);
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("Cache getList failed for key={}: {}", key, e.getMessage());
            return null;
        }
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

    // ── 随机 TTL ──────────────────────────────────────────────────────

    /** TTL 随机抖动因子（±15%） */
    private static final double TTL_JITTER = 0.15;

    /** 为 TTL 加上 ±15% 随机抖动，避免缓存集中过期引发雪崩 */
    private long jitter(long baseSeconds) {
        double factor = 1.0 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * TTL_JITTER;
        return Math.max(1, (long) (baseSeconds * factor));
    }

    // ── ZSET 操作（用于播放历史等排序数据）──────────────────────────────

    /** ZSET 添加成员 */
    public void zadd(String key, double score, String member) {
        try {
            redis.opsForZSet().add(key, member, score);
        } catch (Exception e) {
            log.warn("ZADD failed for key={}: {}", key, e.getMessage());
        }
    }

    /** ZSET 倒序获取成员（score 从高到低），返回 member 集合 */
    public java.util.Set<String> zrevrange(String key, long start, long end) {
        try {
            return redis.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.warn("ZREVRANGE failed for key={}: {}", key, e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /** ZSET 按 score 范围删除（用于清理过期数据） */
    public void zremrangeByScore(String key, double min, double max) {
        try {
            Long removed = redis.opsForZSet().removeRangeByScore(key, min, max);
            if (removed != null && removed > 0) {
                log.debug("ZREMRANGEBYSCORE removed {} entries from key={}", removed, key);
            }
        } catch (Exception e) {
            log.warn("ZREMRANGEBYSCORE failed for key={}: {}", key, e.getMessage());
        }
    }

    /** ZSET 获取成员总数 */
    public long zcard(String key) {
        try {
            Long size = redis.opsForZSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            return 0;
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
    @SuppressWarnings("unchecked")
    public <T> T getWithVersion(String key, Class<T> clazz, long expectedVersion) {
        try {
            String stored = redis.opsForValue().get(key);
            if (stored == null || stored.isEmpty()) return null;
            String json = decompress(stored);
            var javaType = mapper.getTypeFactory().constructParametricType(VersionedWrapper.class, clazz);
            VersionedWrapper<T> wrapper = mapper.readValue(json, javaType);
            if (wrapper == null) return null;
            if (wrapper.version != expectedVersion) {
                log.debug("Cache version mismatch for key={}: cached={}, expected={}", key, wrapper.version, expectedVersion);
                return null;
            }
            return wrapper.data;
        } catch (Exception e) {
            log.warn("Cache getWithVersion failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    // ── 多级缓存统一入口（L1→L2→DB + 分布式锁）────────────────────

    /**
     * 多级缓存读取（单对象）。
     *
     * 数据流：L1(Caffeine) → L2(Redis) → DB（带分布式锁控流）
     * 任一层命中即返回并回填上层。
     *
     * @param key      缓存 key
     * @param clazz    目标类型
     * @param dbLoader 查 DB 的函数（仅缓存未命中时调用）
     * @param <T>      返回值类型
     * @return 缓存数据或 DB 数据，可能为 null
     */
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> dbLoader) {
        // 1. L1 Caffeine
        T l1Result = l1Cache.get(key);
        if (l1Result != null) {
            return l1Result;
        }

        // 2. L2 Redis
        T cached = get(key, clazz);
        if (cached != null) {
            l1Cache.put(key, cached);
            return cached;
        }

        // 3. L2 miss → 分布式锁 + DB
        return lockHelper.withLock(key,
                // double-check：拿到锁后再读一次 L2
                () -> get(key, clazz),
                dbLoader,
                // 回填 L1 + L2
                (data) -> {
                    l1Cache.put(key, data);
                    set(key, data);
                });
    }

    /**
     * 多级缓存读取（单对象，自定义 TTL）。
     *
     * 与 getOrLoad 相同，但使用指定的 TTL 回写 Redis。
     */
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> dbLoader, long ttlSeconds) {
        // 1. L1 Caffeine
        T l1Result = l1Cache.get(key);
        if (l1Result != null) {
            return l1Result;
        }

        // 2. L2 Redis
        T cached = get(key, clazz);
        if (cached != null) {
            l1Cache.put(key, cached);
            return cached;
        }

        // 3. L2 miss → 分布式锁 + DB
        return lockHelper.withLock(key,
                () -> get(key, clazz),
                dbLoader,
                (data) -> {
                    l1Cache.put(key, data);
                    set(key, data, ttlSeconds);
                });
    }

    /**
     * 多级缓存读取（List，自定义 TTL）。
     */
    public <T> List<T> getOrLoadList(String key, Class<T> elementClass, Supplier<List<T>> dbLoader) {
        // 1. L1 Caffeine
        List<T> l1Result = l1Cache.get(key);
        if (l1Result != null) {
            return l1Result;
        }

        // 2. L2 Redis
        List<T> cached = getList(key, elementClass);
        if (cached != null) {
            l1Cache.put(key, cached);
            return cached;
        }

        // 3. L2 miss → 分布式锁 + DB
        return lockHelper.withLock(key,
                () -> getList(key, elementClass),
                dbLoader,
                (data) -> {
                    l1Cache.put(key, data);
                    set(key, data);
                });
    }

    /**
     * 多级缓存读取（List，自定义 TTL）。
     */
    public <T> List<T> getOrLoadList(String key, Class<T> elementClass, Supplier<List<T>> dbLoader, long ttlSeconds) {
        // 1. L1 Caffeine
        List<T> l1Result = l1Cache.get(key);
        if (l1Result != null) {
            return l1Result;
        }

        // 2. L2 Redis
        List<T> cached = getList(key, elementClass);
        if (cached != null) {
            l1Cache.put(key, cached);
            return cached;
        }

        // 3. L2 miss → 分布式锁 + DB
        return lockHelper.withLock(key,
                () -> getList(key, elementClass),
                dbLoader,
                (data) -> {
                    l1Cache.put(key, data);
                    set(key, data, ttlSeconds);
                });
    }

    /**
     * 使指定 key 的 L1 + L2 缓存同时失效。
     */
    public void evictBoth(String key) {
        l1Cache.evict(key);
        delete(key);
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
}
