package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.User.UserArtistFollowMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 关注服务（Redis SET 实时 + 异步 MySQL 持久化 + 定时对账）
 *
 * 写入路径：follow/unfollow → Redis SET（同步，~1ms）→ MySQL（异步，不阻塞）
 * 读取路径：Redis SISMEMBER / SCARD / SMEMBERS（实时，~1ms）
 *
 * Redis Key:
 *   spotify:follow:artist:{artistId}  — SET of userIds（艺人粉丝）
 *   spotify:follow:user:{userId}      — SET of artistIds（用户关注）
 *   spotify:follow:artists            — SET of artistIds（有关注活动的艺人，用于对账遍历）
 */
@Service
public class FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowService.class);

    static final String KEY_FOLLOW_ARTIST = "spotify:follow:artist:%d";
    static final String KEY_FOLLOW_USER = "spotify:follow:user:%d";
    static final String KEY_FOLLOW_ARTISTS = "spotify:follow:artists";

    private static final long TTL_DAYS = 365; // 关注关系长期有效

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private UserArtistFollowMapper followMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private CacheService cacheService;

    // ── 关注 / 取关 ──────────────────────────────────────────────────────

    /** 关注艺人。Redis 同步写 + MySQL 异步写。 */
    public void follow(Long userId, Integer artistId) {
        String artistKey = String.format(KEY_FOLLOW_ARTIST, artistId);
        String userKey = String.format(KEY_FOLLOW_USER, userId);

        redis.opsForSet().add(artistKey, String.valueOf(userId));
        redis.opsForSet().add(userKey, String.valueOf(artistId));
        redis.opsForSet().add(KEY_FOLLOW_ARTISTS, String.valueOf(artistId));

        redis.expire(artistKey, TTL_DAYS, TimeUnit.DAYS);
        redis.expire(userKey, TTL_DAYS, TimeUnit.DAYS);
        redis.expire(KEY_FOLLOW_ARTISTS, TTL_DAYS, TimeUnit.DAYS);

        // 异步写 MySQL
        persistFollow(userId, artistId);

        // 清除相关缓存
        invalidateCaches(userId, artistId);
        log.debug("Follow: userId={} artistId={}", userId, artistId);
    }

    /** 取消关注。Redis 同步删 + MySQL 异步删。 */
    public void unfollow(Long userId, Integer artistId) {
        String artistKey = String.format(KEY_FOLLOW_ARTIST, artistId);
        String userKey = String.format(KEY_FOLLOW_USER, userId);

        redis.opsForSet().remove(artistKey, String.valueOf(userId));
        redis.opsForSet().remove(userKey, String.valueOf(artistId));

        // 异步删 MySQL
        persistUnfollow(userId, artistId);

        // 清除相关缓存
        invalidateCaches(userId, artistId);
        log.debug("Unfollow: userId={} artistId={}", userId, artistId);
    }

    // ── 查询 ────────────────────────────────────────────────────────────

    /** 检查是否已关注 */
    public boolean isFollowing(Long userId, Integer artistId) {
        try {
            String artistKey = String.format(KEY_FOLLOW_ARTIST, artistId);
            Boolean member = redis.opsForSet().isMember(artistKey, String.valueOf(userId));
            return Boolean.TRUE.equals(member);
        } catch (Exception e) {
            // Redis 挂了降级到 MySQL
            log.warn("isFollowing Redis failed, fallback to MySQL: {}", e.getMessage());
            return followMapper.count(userId, artistId) > 0;
        }
    }

    /** 获取艺人粉丝数 */
    public int getFollowerCount(Integer artistId) {
        try {
            String artistKey = String.format(KEY_FOLLOW_ARTIST, artistId);
            Long size = redis.opsForSet().size(artistKey);
            if (size != null && size > 0) return size.intValue();
        } catch (Exception e) {
            log.warn("getFollowerCount Redis failed: {}", e.getMessage());
        }
        return followMapper.countFollowersByArtistId(artistId);
    }

    /** 获取用户关注数 */
    public int getFollowingCount(Long userId) {
        try {
            String userKey = String.format(KEY_FOLLOW_USER, userId);
            Long size = redis.opsForSet().size(userKey);
            if (size != null && size > 0) return size.intValue();
        } catch (Exception e) {
            log.warn("getFollowingCount Redis failed: {}", e.getMessage());
        }
        // Redis 空则降级 MySQL
        List<Artist> artists = followMapper.findFollowedArtists(userId);
        return artists != null ? artists.size() : 0;
    }

    /** 获取用户关注的所有艺人 ID 列表 */
    public Set<String> getFollowedArtistIds(Long userId) {
        try {
            String userKey = String.format(KEY_FOLLOW_USER, userId);
            Set<String> members = redis.opsForSet().members(userKey);
            if (members != null && !members.isEmpty()) return members;
        } catch (Exception e) {
            log.warn("getFollowedArtistIds Redis failed: {}", e.getMessage());
        }
        // 降级 MySQL
        List<Artist> artists = followMapper.findFollowedArtists(userId);
        if (artists == null) return Collections.emptySet();
        return artists.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.toSet());
    }

    // ── 异步 MySQL 持久化 ────────────────────────────────────────────────

    @Async("playbackExecutor")
    void persistFollow(Long userId, Integer artistId) {
        try {
            followMapper.insert(userId, artistId);
        } catch (Exception e) {
            log.warn("persistFollow MySQL failed: userId={} artistId={} error={}",
                    userId, artistId, e.getMessage());
        }
    }

    @Async("playbackExecutor")
    void persistUnfollow(Long userId, Integer artistId) {
        try {
            followMapper.delete(userId, artistId);
        } catch (Exception e) {
            log.warn("persistUnfollow MySQL failed: userId={} artistId={} error={}",
                    userId, artistId, e.getMessage());
        }
    }

    // ── 定时对账（每 5 分钟同步 fansCount 到 artists 表） ─────────────────

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncFansCount() {
        Set<String> artistIds = redis.opsForSet().members(KEY_FOLLOW_ARTISTS);
        if (artistIds == null || artistIds.isEmpty()) return;

        log.info("syncFansCount: syncing {} artists", artistIds.size());
        int updated = 0;
        for (String idStr : artistIds) {
            try {
                int artistId = Integer.parseInt(idStr);
                String artistKey = String.format(KEY_FOLLOW_ARTIST, artistId);
                Long count = redis.opsForSet().size(artistKey);
                if (count == null) continue;

                Artist artist = new Artist();
                artist.setId(artistId);
                artist.setFansCount(count.intValue());
                artistMapper.update(artist);
                updated++;
            } catch (Exception e) {
                log.warn("syncFansCount failed for artistId={}: {}", idStr, e.getMessage());
            }
        }
        log.info("syncFansCount: updated {} artists", updated);
    }

    // ── 缓存失效 ────────────────────────────────────────────────────────

    private void invalidateCaches(Long userId, Integer artistId) {
        cacheService.evictBoth(String.format(CacheService.KEY_HOME, userId));
        cacheService.evictBoth(String.format(CacheService.KEY_PROFILE, userId));
        cacheService.evictBoth(String.format(CacheService.KEY_ARTIST, artistId));
    }
}
