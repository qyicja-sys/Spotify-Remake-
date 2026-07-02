package com.ty1l.spotify_remake.Service;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * L1 本地进程内缓存（Caffeine）
 *
 * 按 key 前缀路由到不同的 Caffeine Cache 实例，类型隔离、独立容量控制。
 * L1 TTL 固定 5 分钟（CaffeineConfig 配置），比 Redis L2 TTL 短，减少脏读窗口。
 *
 * 数据流：L1 → L2(Redis) → DB
 * 任一层命中即返回并回填上层。
 */
@Service
public class L1CacheService {

    private static final Logger log = LoggerFactory.getLogger(L1CacheService.class);

    private final Cache<String, Object> songCache;
    private final Cache<String, Object> artistCache;
    private final Cache<String, Object> albumCache;
    private final Cache<String, Object> lyricsCache;
    private final Cache<String, Object> externalCache;
    private final Cache<String, Object> homeCache;
    private final Cache<String, Object> profileCache;
    private final Cache<String, String> stringCache;

    public L1CacheService(
            @Qualifier("songCache") Cache<String, Object> songCache,
            @Qualifier("artistCache") Cache<String, Object> artistCache,
            @Qualifier("albumCache") Cache<String, Object> albumCache,
            @Qualifier("lyricsCache") Cache<String, Object> lyricsCache,
            @Qualifier("externalCache") Cache<String, Object> externalCache,
            @Qualifier("homeCache") Cache<String, Object> homeCache,
            @Qualifier("profileCache") Cache<String, Object> profileCache,
            @Qualifier("stringCache") Cache<String, String> stringCache) {
        this.songCache = songCache;
        this.artistCache = artistCache;
        this.albumCache = albumCache;
        this.lyricsCache = lyricsCache;
        this.externalCache = externalCache;
        this.homeCache = homeCache;
        this.profileCache = profileCache;
        this.stringCache = stringCache;
    }

    // ── 路由 ──────────────────────────────────────────────────────────

    /** 根据 key 前缀路由到对应的 Caffeine Cache */
    @SuppressWarnings("unchecked")
    private <T> Cache<String, T> selectCache(String key) {
        if (key.contains(":song:"))       return (Cache<String, T>) songCache;
        if (key.contains(":artist:"))     return (Cache<String, T>) artistCache;
        if (key.contains(":album:"))      return (Cache<String, T>) albumCache;
        if (key.contains(":lyrics:"))     return (Cache<String, T>) lyricsCache;
        if (key.contains(":external:"))   return (Cache<String, T>) externalCache;
        if (key.contains(":home:"))       return (Cache<String, T>) homeCache;
        if (key.contains(":profile:"))    return (Cache<String, T>) profileCache;
        if (key.contains(":stream:"))     return (Cache<String, T>) (Cache) stringCache;
        // 默认走 song cache（兜底，避免额外开销）
        return (Cache<String, T>) songCache;
    }

    // ── 读写 ──────────────────────────────────────────────────────────

    /** 从 L1 读取 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            Cache<String, Object> cache = selectCache(key);
            Object val = cache.getIfPresent(key);
            if (val != null) {
                log.debug("L1 cache hit: {}", key);
                return (T) val;
            }
        } catch (Exception e) {
            log.warn("L1 get failed for key={}: {}", key, e.getMessage());
        }
        return null;
    }

    /** 写入 L1 */
    public void put(String key, Object value) {
        if (value == null) return;
        try {
            selectCache(key).put(key, value);
        } catch (Exception e) {
            log.warn("L1 put failed for key={}: {}", key, e.getMessage());
        }
    }

    /** 从 L1 删除 */
    public void evict(String key) {
        try {
            selectCache(key).invalidate(key);
        } catch (Exception e) {
            log.warn("L1 evict failed for key={}: {}", key, e.getMessage());
        }
    }
}
