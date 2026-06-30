package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.UserPlaybackHistory;
import com.ty1l.spotify_remake.Mapper.User.PlaybackHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 播放历史异步持久化（独立 Bean 确保 @Async 代理生效）
 */
@Service
public class PlaybackPersistence {

    private static final Logger log = LoggerFactory.getLogger(PlaybackPersistence.class);

    @Autowired
    private PlaybackHistoryMapper playbackHistoryMapper;

    @Async("playbackExecutor")
    public void persistPlayback(Long userId, Long songId, Long artistId, long timestampMillis) {
        try {
            UserPlaybackHistory history = new UserPlaybackHistory();
            history.setUserId(userId);
            history.setSongId(songId);
            history.setArtistId(artistId);
            history.setPlayedAt(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestampMillis),
                    ZoneId.systemDefault()));
            playbackHistoryMapper.insertPlaybackHistory(history);
        } catch (Exception e) {
            log.warn("Async persist playback failed: userId={} songId={} artistId={}: {}",
                    userId, songId, artistId, e.getMessage());
        }
    }
}
