package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 月听众数服务（Redis Set 实时去重 + 定时批量同步 MySQL）
 *
 * 写入路径：用户播放歌曲 → SADD 到当月艺人 SET（去重，O(1)）
 * 同步路径：每 5 分钟 @Scheduled → SCARD + 批量 UPDATE artists.monthlyListeners
 *
 * Redis Key:
 *   spotify:listeners:{yyyy-MM}:artist:{artistId}  — SET of userIds（唯一听众）
 *   spotify:listeners:{yyyy-MM}:artists            — SET of artistIds（本月活跃艺人）
 */
@Service
public class MonthlyListenersService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyListenersService.class);

    /** 每月听众 SET key 模板 */
    static final String KEY_LISTENERS_ARTIST = "spotify:listeners:%s:artist:%d";
    /** 本月活跃艺人 SET key 模板 */
    static final String KEY_LISTENERS_ACTIVE = "spotify:listeners:%s:artists";

    /** TTL：35 天（略长于一个月，跨月自动过期） */
    private static final long TTL_DAYS = 35;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private CacheService cacheService;

    // ── 实时写入 ────────────────────────────────────────────────────────

    /**
     * 记录用户收听了某艺人的歌曲。
     * 同一用户同月对同一艺人只计一次（Redis SET 天然去重）。
     *
     * @param userId   用户 ID
     * @param artistId 艺人 ID
     */
    public void recordListen(Long userId, Long artistId) {
        try {
            String month = currentMonth();
            String artistKey = String.format(KEY_LISTENERS_ARTIST, month, artistId);
            String activeKey = String.format(KEY_LISTENERS_ACTIVE, month);

            // SADD 返回 1 表示新成员，0 表示已存在（已计入，忽略）
            Long added = redis.opsForSet().add(artistKey, String.valueOf(userId));
            if (added != null && added > 0) {
                // 首次收听：标记该艺人为活跃
                redis.opsForSet().add(activeKey, String.valueOf(artistId));
                log.debug("New monthly listener: userId={} artistId={} month={}", userId, artistId, month);
            }

            // 续 TTL（每次写入都续期，活跃 key 不会过期）
            redis.expire(artistKey, TTL_DAYS, TimeUnit.DAYS);
            redis.expire(activeKey, TTL_DAYS, TimeUnit.DAYS);

        } catch (Exception e) {
            log.warn("recordListen failed: userId={} artistId={} error={}", userId, artistId, e.getMessage());
        }
    }

    // ── 定时同步 ────────────────────────────────────────────────────────

    /**
     * 每 5 分钟将 Redis 中的月听众数批量同步到 MySQL。
     *
     * 流程：
     * 1. 读取本月活跃艺人集合
     * 2. 逐个 SCARD 获取听众数
     * 3. 批量 UPDATE artists.monthlyListeners
     * 4. 清除对应 artist 缓存，触发下次请求重建
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncToDatabase() {
        String month = currentMonth();
        String activeKey = String.format(KEY_LISTENERS_ACTIVE, month);

        Set<String> artistIds = redis.opsForSet().members(activeKey);
        if (artistIds == null || artistIds.isEmpty()) {
            log.debug("syncToDatabase: no active artists for month={}", month);
            return;
        }

        log.info("syncToDatabase: syncing {} active artists for month={}", artistIds.size(), month);
        int updated = 0;

        for (String artistIdStr : artistIds) {
            try {
                int artistId = Integer.parseInt(artistIdStr);
                String artistKey = String.format(KEY_LISTENERS_ARTIST, month, artistId);

                Long count = redis.opsForSet().size(artistKey);
                if (count == null) continue;

                // 更新 MySQL
                Artist artist = new Artist();
                artist.setId(artistId);
                artist.setMonthlyListeners(count.intValue());
                artistMapper.update(artist);

                // 清除 artist 缓存，下次请求时重建（拿到最新 monthlyListeners）
                String cacheKey = String.format(CacheService.KEY_ARTIST, artistId);
                cacheService.delete(cacheKey);

                updated++;
            } catch (Exception e) {
                log.warn("syncToDatabase: failed for artistId={}: {}", artistIdStr, e.getMessage());
            }
        }

        log.info("syncToDatabase: updated {} artists for month={}", updated, month);
    }

    // ── 工具方法 ────────────────────────────────────────────────────────

    private static String currentMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
