package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.*;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongPlayLogMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 全球排行榜服务 — Redis ZSET 实时排名 + 逻辑过期缓存 + ZSET 增量检测 + 每日 MySQL 持久化
 *
 * <h3>架构</h3>
 * <pre>
 * 播放 → ZINCRBY (Redis ZSET, ~1ms)  ← 零 MySQL 写入压力
 * 读取 → 逻辑过期缓存 (无物理 TTL) → 过期或 ZSET 有新条目时同步重建
 * 持久化 → 每日 00:00:00 Redis ZSET 批量 UPSERT 到 MySQL
 * </pre>
 *
 * <h3>Redis Key</h3>
 * <ul>
 *   <li>{@code spotify:leaderboard:global} — ZSET, member=songId, score=playCount</li>
 *   <li>{@code spotify:leaderboard:global:cache} — String(JSON), {@link LeaderboardCache}</li>
 * </ul>
 */
@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private static final int TOP_N = 50;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SongPlayLogMapper songPlayLogMapper;

    @Autowired
    private SongService songService;

    @Autowired
    private SongArtistMapper songArtistMapper;

    @Autowired
    private ArtistMapper artistMapper;

    // ── 播放记录（实时写入）──────────────────────────────────────────────

    /**
     * 记录一次歌曲播放 — 仅写 Redis ZSET，MySQL 由零点对账批量同步
     *
     * @param songId 歌曲 ID
     */
    public void recordPlay(Integer songId) {
        if (songId == null) return;
        try {
            redis.opsForZSet().incrementScore(CacheService.KEY_LEADERBOARD_ZSET, String.valueOf(songId), 1);
        } catch (Exception e) {
            log.warn("Leaderboard recordPlay failed for songId={}: {}", songId, e.getMessage());
        }
    }

    // ── 排行榜查询（逻辑过期防缓存击穿）──────────────────────────────────

    /**
     * 获取全球 Top 50 排行榜。
     *
     * <p>逻辑过期 + ZSET 增量检测：缓存无物理 TTL，过期时间存储在 Value 中。
     * 当缓存逻辑过期或 ZSET 有新条目加入时，同步重建缓存。
     *
     * @return 排行榜歌曲列表（排名 1-50）
     */
    public List<LeaderboardSongVO> getGlobalTop50() {
        // 1. 读取逻辑过期缓存（CacheService.get 自动处理解压 + 反序列化）
        LeaderboardCache cache = cacheService.get(CacheService.KEY_LEADERBOARD_CACHE, LeaderboardCache.class);

        // 2. 缓存不存在 → 同步构建（首次请求）
        if (cache == null) {
            log.info("Leaderboard cache miss — building synchronously");
            return buildAndCache();
        }

        // 3. 检测 ZSET 是否有新条目加入（缓存快照可能落后于实时 ZSET）
        int cacheSize = cache.getSongs() != null ? cache.getSongs().size() : 0;
        Long zsetSize = redis.opsForZSet().zCard(CacheService.KEY_LEADERBOARD_ZSET);
        boolean hasNewEntries = zsetSize != null && zsetSize > cacheSize;

        // 4. 缓存未逻辑过期且无新条目 → 直接返回
        if (cache.getExpireTime() > System.currentTimeMillis() && !hasNewEntries) {
            return cache.getSongs();
        }

        // 5. 缓存逻辑过期或有新条目 → 同步重建（保证数据实时性）
        if (hasNewEntries) {
            log.info("Leaderboard ZSET has {} entries but cache has {} — rebuilding", zsetSize, cacheSize);
        } else {
            log.info("Leaderboard cache logically expired (expireTime={}, now={}) — rebuilding",
                    cache.getExpireTime(), System.currentTimeMillis());
        }
        return buildAndCache();
    }

    /**
     * 从 Redis ZSET 构建 Top 50 并写入逻辑过期缓存
     */
    private List<LeaderboardSongVO> buildAndCache() {
        try {
            // 1. 从 ZSET 获取 Top 50（score 降序）
            Set<String> topSongIds = redis.opsForZSet()
                    .reverseRange(CacheService.KEY_LEADERBOARD_ZSET, 0, TOP_N - 1);

            if (topSongIds == null || topSongIds.isEmpty()) {
                log.info("Leaderboard ZSET is empty — trying fallback from MySQL");
                return fallbackFromMySQL();
            }

            // 2. 获取每个 songId 的 score，构建 VO
            List<LeaderboardSongVO> songs = new ArrayList<>();
            int rank = 1;
            for (String songIdStr : topSongIds) {
                Integer songId = Integer.valueOf(songIdStr);
                Double score = redis.opsForZSet().score(CacheService.KEY_LEADERBOARD_ZSET, songIdStr);
                long playCount = score != null ? score.longValue() : 0;

                Song song = songService.findById(songId);
                if (song == null || isInvalidSong(song)) continue;

                songs.add(buildVO(song, playCount, rank++));
            }

            // 3. 写入逻辑过期缓存（物理 TTL = -1，逻辑过期时间 = 次日 00:00:00）
            long tomorrowMidnight = LocalDate.now().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            LeaderboardCache cache = new LeaderboardCache();
            cache.setSongs(songs);
            cache.setExpireTime(tomorrowMidnight);
            cache.setRefreshTime(System.currentTimeMillis());

            cacheService.setPermanent(CacheService.KEY_LEADERBOARD_CACHE, cache);
            log.info("Leaderboard cache built: {} songs, logicalExpire={}", songs.size(), tomorrowMidnight);

            return songs;
        } catch (Exception e) {
            log.error("Failed to build leaderboard cache: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Redis ZSET 为空时的 MySQL 兜底查询
     */
    private List<LeaderboardSongVO> fallbackFromMySQL() {
        try {
            List<Map<String, Object>> rows = songPlayLogMapper.aggregateByDate(LocalDate.now(), TOP_N);
            if (rows == null || rows.isEmpty()) {
                // 尝试查昨天的数据
                rows = songPlayLogMapper.aggregateByDate(LocalDate.now().minusDays(1), TOP_N);
            }
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }

            List<LeaderboardSongVO> songs = new ArrayList<>();
            int rank = 1;
            for (Map<String, Object> row : rows) {
                Integer songId = ((Number) row.get("songId")).intValue();
                Long playCount = ((Number) row.get("playCount")).longValue();

                Song song = songService.findById(songId);
                if (song == null || isInvalidSong(song)) continue;

                songs.add(buildVO(song, playCount, rank++));
            }

            // 回写 ZSET 和缓存
            for (LeaderboardSongVO vo : songs) {
                redis.opsForZSet().add(CacheService.KEY_LEADERBOARD_ZSET, String.valueOf(vo.getId()), vo.getPlayCount());
            }
            long tomorrowMidnight = LocalDate.now().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            LeaderboardCache cache = new LeaderboardCache(songs, tomorrowMidnight, System.currentTimeMillis());
            cacheService.setPermanent(CacheService.KEY_LEADERBOARD_CACHE, cache);

            return songs;
        } catch (Exception e) {
            log.error("Fallback from MySQL failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据歌曲实体构建 LeaderboardSongVO（填充艺人名）
     */
    private LeaderboardSongVO buildVO(Song song, long playCount, int rank) {
        LeaderboardSongVO vo = new LeaderboardSongVO();
        vo.setId(song.getId());
        vo.setTitle(song.getTitle());
        vo.setCoverUrl(song.getCoverUrl());
        vo.setCoverNetworkUrl(song.getCoverNetworkUrl());
        vo.setDuration(song.getDuration());
        vo.setPlayCount(playCount);
        vo.setRank(rank);
        vo.setExternalSource(song.getExternalSource());
        vo.setExternalId(song.getExternalId());
        vo.setPicId(song.getPicId());

        List<SongArtist> saList = songArtistMapper.findBySongId(song.getId());
        if (saList != null && !saList.isEmpty()) {
            String names = saList.stream()
                    .sorted(Comparator.comparingInt(sa -> sa.getIsMain() != null && sa.getIsMain() == 1 ? 0 : 1))
                    .map(sa -> {
                        Artist a = artistMapper.findById(sa.getArtistId());
                        return a != null ? a.getName() : "";
                    })
                    .filter(n -> !n.isEmpty())
                    .collect(Collectors.joining(", "));
            vo.setArtistName(names);
        }
        return vo;
    }

    /**
     * 过滤无效歌曲（封面为空的外部歌曲不展示）
     */
    private boolean isInvalidSong(Song song) {
        return song.getExternalSource() != null && !song.getExternalSource().isEmpty()
                && (song.getCoverNetworkUrl() == null || song.getCoverNetworkUrl().isEmpty());
    }

    // ── 每日对账（00:00:00）───────────────────────────────────────────────

    /**
     * 每日零点将 Redis ZSET 累计播放量批量同步到 MySQL。
     *
     * <ol>
     *   <li>从 Redis ZSET 读取全部条目（songId + 累计播放量）</li>
     *   <li>批量 UPSERT 到 MySQL song_play_log（play_date = 昨天）</li>
     *   <li>重建逻辑过期缓存</li>
     * </ol>
     *
     * <p>ZSET 保留不重置，维持累计排名。MySQL 仅作持久化备份。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyReconciliation() {
        log.info("=== Daily leaderboard reconciliation START ===");
        long start = System.currentTimeMillis();
        try {
            // 1. 从 Redis ZSET 读取全部条目及分数
            Set<ZSetOperations.TypedTuple<String>> allEntries = redis.opsForZSet()
                    .reverseRangeWithScores(CacheService.KEY_LEADERBOARD_ZSET, 0, -1);

            if (allEntries == null || allEntries.isEmpty()) {
                log.info("Leaderboard ZSET is empty — nothing to persist");
                return;
            }

            // 2. 批量 UPSERT 到 MySQL（累计播放量覆盖写入）
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<SongPlayLog> logs = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> entry : allEntries) {
                SongPlayLog logEntry = new SongPlayLog();
                logEntry.setSongId(Integer.valueOf(entry.getValue()));
                logEntry.setPlayCount(entry.getScore() != null ? entry.getScore().longValue() : 0L);
                logEntry.setPlayDate(yesterday);
                logs.add(logEntry);
            }
            int dbRows = songPlayLogMapper.batchUpsert(logs);

            // 3. 重建缓存
            buildAndCache();

            long elapsed = System.currentTimeMillis() - start;
            log.info("=== Daily leaderboard reconciliation DONE: {} songs → MySQL ({} rows), {}ms ===",
                    logs.size(), dbRows, elapsed);
        } catch (Exception e) {
            log.error("Daily leaderboard reconciliation failed: {}", e.getMessage(), e);
        }
    }
}
