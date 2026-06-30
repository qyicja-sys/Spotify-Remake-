-- 收藏歌单表：用户收藏其他用户/系统歌单到自己的音乐库
CREATE TABLE IF NOT EXISTS user_collected_playlists (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    userId     BIGINT UNSIGNED NOT NULL COMMENT '收藏者用户ID',
    playlistId BIGINT UNSIGNED NOT NULL COMMENT '被收藏的歌单ID',
    collectedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_playlist (userId, playlistId),
    INDEX idx_userId (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏歌单表';
