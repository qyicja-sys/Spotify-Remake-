package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Entity.Public.SongArtist;
import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.User.RecentArtistVO;
import com.ty1l.spotify_remake.Entity.User.RecentlyPlayedSongVO;
import com.ty1l.spotify_remake.Entity.User.UserPlaybackHistory;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Mapper.User.PlaybackHistoryMapper;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.LeaderboardService;
import com.ty1l.spotify_remake.Service.Public.MonthlyListenersService;
import com.ty1l.spotify_remake.Service.Public.SongService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 播放历史服务（Redis ZSET + 异步 MySQL 持久化）
 *
 * 写入路径：前端异步 POST → Redis ZSET（同步，~1ms）→ MySQL（异步，不阻塞）
 * 读取路径：Redis ZSET（ZREVRANGE，~1ms）→ 补全歌曲/艺人详情
 */
@Service
public class PlaybackHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PlaybackHistoryService.class);

    /** ZSET 最大保留条数 */
    private static final int MAX_ZSET_SIZE = 100;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SongArtistMapper songArtistMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private PlaybackHistoryMapper playbackHistoryMapper;

    @Autowired
    private PlaybackPersistence persistence;

    @Autowired
    private SongService songService;

    @Autowired
    private MonthlyListenersService monthlyListenersService;

    @Autowired
    private LeaderboardService leaderboardService;

    /**
     * 记录播放历史（同步写 Redis ZSET + 异步落库 MySQL）
     *
     * @param userId  用户 ID
     * @param songId  歌曲 ID
     */
    public void recordPlayback(Long userId, Integer songId) {
        long now = System.currentTimeMillis();
        String songKey = String.format(CacheService.KEY_PLAYBACK_SONGS, userId);
        String artistKey = String.format(CacheService.KEY_PLAYBACK_ARTISTS, userId);

        // 1. 同步写 Redis ZSET（快速，~1ms）
        cacheService.zadd(songKey, now, String.valueOf(songId));
        // 1a. 同步更新全球排行榜 ZSET（ZINCRBY，~1ms）
        leaderboardService.recordPlay(songId);
        log.info("Playback recorded to Redis: userId={} songId={} songKey={}", userId, songId, songKey);

        // 2. 查找歌曲关联的所有艺人，写入艺人 ZSET
        List<SongArtist> songArtists = songArtistMapper.findBySongId(songId);
        if (songArtists != null && !songArtists.isEmpty()) {
            for (SongArtist sa : songArtists) {
                cacheService.zadd(artistKey, now, String.valueOf(sa.getArtistId()));
                // 3. 异步落库 MySQL（不阻塞主线程）
                persistence.persistPlayback(userId, songId.longValue(), sa.getArtistId().longValue(), now);
                // 4. 实时记录月听众（Redis SET 去重）
                monthlyListenersService.recordListen(userId, sa.getArtistId());
            }
            log.info("Playback artist ZSET updated: userId={} songId={} artistCount={}", userId, songId, songArtists.size());
        } else {
            log.warn("No song_artist mapping found for songId={}, playback recorded without artist", songId);
        }

        // 4. 裁剪 ZSET，保留最近 MAX_ZSET_SIZE 条
        long threshold = now - (365L * 24 * 3600 * 1000); // 一年前的数据可清理
        cacheService.zremrangeByScore(songKey, 0, threshold);
        cacheService.zremrangeByScore(artistKey, 0, threshold);
    }

    /**
     * Redis 冷启动：从 MySQL user_playback_history 恢复 ZSET 数据。
     * 当 Redis ZSET 为空但 MySQL 有记录时调用此方法回写。
     */
    public void syncMySQLToRedis(Long userId) {
        List<UserPlaybackHistory> histories = playbackHistoryMapper.selectAllPlaybackHistory(userId);
        if (histories == null || histories.isEmpty()) {
            log.debug("syncMySQLToRedis: no MySQL records for userId={}", userId);
            return;
        }

        String songKey = String.format(CacheService.KEY_PLAYBACK_SONGS, userId);
        String artistKey = String.format(CacheService.KEY_PLAYBACK_ARTISTS, userId);

        for (UserPlaybackHistory h : histories) {
            long score = h.getPlayedAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            cacheService.zadd(songKey, score, String.valueOf(h.getSongId()));
            if (h.getArtistId() != null) {
                cacheService.zadd(artistKey, score, String.valueOf(h.getArtistId()));
            }
        }
        log.info("syncMySQL→Redis complete: userId={} records={}", userId, histories.size());
    }

    /**
     * 获取用户最近播放的歌曲 ID 列表（按播放时间倒序）
     */
    public List<Long> getRecentSongIds(Long userId, int limit) {
        String key = String.format(CacheService.KEY_PLAYBACK_SONGS, userId);
        Set<String> members = cacheService.zrevrange(key, 0, limit - 1);
        if (members == null || members.isEmpty()) return Collections.emptyList();
        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户最近播放的艺人 ID 列表（按播放时间倒序）
     */
    public List<Long> getRecentArtistIds(Long userId, int limit) {
        String key = String.format(CacheService.KEY_PLAYBACK_ARTISTS, userId);
        Set<String> members = cacheService.zrevrange(key, 0, limit - 1);
        if (members == null || members.isEmpty()) return Collections.emptyList();
        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 列表组装 RecentlyPlayedSongVO 列表（查歌曲缓存/DB + 艺人名）
     */
    public List<RecentlyPlayedSongVO> buildRecentSongs(List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) return Collections.emptyList();
        List<RecentlyPlayedSongVO> result = new ArrayList<>();
        // 去重 + 保持 ZSET 顺序
        Set<Long> seen = new LinkedHashSet<>(songIds);
        for (Long songId : seen) {
            Song song = songService.findById(songId.intValue());
            if (song == null) continue;
            // 过滤封面为空的外部歌曲
            if (song.getExternalSource() != null && !song.getExternalSource().isEmpty()
                    && (song.getCoverNetworkUrl() == null || song.getCoverNetworkUrl().isEmpty())) {
                continue;
            }
            RecentlyPlayedSongVO vo = new RecentlyPlayedSongVO();
            vo.setId(songId);
            vo.setTitle(song.getTitle());
            vo.setDuration(song.getDuration());
            vo.setCoverUrl(song.getCoverUrl());
            vo.setCoverNetworkUrl(song.getCoverNetworkUrl());
            vo.setExternalSource(song.getExternalSource());
            vo.setExternalId(song.getExternalId());
            vo.setPicId(song.getPicId());
            // 查询艺人名
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
            result.add(vo);
        }
        return result;
    }

    /**
     * 根据 ID 列表组装 RecentArtistVO 列表
     */
    public List<RecentArtistVO> buildRecentArtists(List<Long> artistIds) {
        if (artistIds == null || artistIds.isEmpty()) return Collections.emptyList();
        // 去重 + 保持 ZSET 顺序
        Set<Long> seen = new LinkedHashSet<>(artistIds);
        List<RecentArtistVO> result = new ArrayList<>();
        for (Long artistId : seen) {
            Artist a = artistMapper.findById(artistId.intValue());
            if (a == null) continue;
            // 同艺人名去重
            boolean duplicate = result.stream().anyMatch(r -> r.getName().equals(a.getName()));
            if (duplicate) continue;
            RecentArtistVO vo = new RecentArtistVO();
            vo.setId(artistId);
            vo.setName(a.getName());
            vo.setAvatarUrl(a.getAvatarUrl());
            vo.setAvatarNetworkUrl(a.getAvatarNetworkUrl());
            result.add(vo);
        }
        return result;
    }

}
