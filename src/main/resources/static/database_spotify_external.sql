-- songs 表新增 external_source 和 external_id 字段，支持外部API歌曲
ALTER TABLE songs ADD COLUMN external_source VARCHAR(50) NULL COMMENT '外部来源（如 gdmusic）' AFTER lyrics;
ALTER TABLE songs ADD COLUMN external_id VARCHAR(100) NULL COMMENT '外部歌曲ID' AFTER external_source;

-- user_playback_history 表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS user_playback_history (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL COMMENT '用户ID',
    song_id    BIGINT UNSIGNED NOT NULL COMMENT '歌曲ID',
    artist_id  BIGINT UNSIGNED NOT NULL COMMENT '歌手ID',
    played_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
    INDEX idx_user_played (user_id, played_at),
    INDEX idx_song (song_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户播放历史';
-- songs 表新增 pic_id 字段
ALTER TABLE songs ADD COLUMN pic_id VARCHAR(100) NULL COMMENT '外部封面图片ID' AFTER external_id;
