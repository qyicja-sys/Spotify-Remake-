-- 专辑表：艺术家可以创建自己的专辑，歌曲通过 albumId 关联
CREATE TABLE IF NOT EXISTS albums (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '专辑唯一ID',
    artistId        BIGINT UNSIGNED NOT NULL COMMENT '创建该专辑的艺术家ID',
    name            VARCHAR(255) NOT NULL COMMENT '专辑名称',
    coverUrl        VARCHAR(500) NULL COMMENT '专辑封面URL（本地）',
    coverNetworkUrl VARCHAR(500) NULL COMMENT '专辑封面URL（网络）',
    description     TEXT NULL COMMENT '专辑简介/描述',
    type            TINYINT DEFAULT 2 NOT NULL COMMENT '专辑类型：0-单曲Single，1-EP，2-专辑Album，3-精选集Compilation',
    releaseDate     DATE NULL COMMENT '发行日期',
    createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    INDEX idx_artist_id (artistId),
    INDEX idx_release_date (releaseDate),
    CONSTRAINT fk_album_artist FOREIGN KEY (artistId) REFERENCES artists(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专辑表';

-- songs 表新增 albumId 字段，关联专辑
ALTER TABLE songs ADD COLUMN albumId BIGINT UNSIGNED NULL COMMENT '所属专辑ID，NULL表示单曲/未归入专辑' AFTER artistId;
ALTER TABLE songs ADD INDEX idx_album_id (albumId);
ALTER TABLE songs ADD CONSTRAINT fk_song_album FOREIGN KEY (albumId) REFERENCES albums(id) ON DELETE SET NULL;

-- 为各表新增网络图片 URL 字段
ALTER TABLE artists   ADD COLUMN avatarNetworkUrl     VARCHAR(500) NULL COMMENT '歌手头像URL（网络）' AFTER avatarUrl;
ALTER TABLE songs     ADD COLUMN coverNetworkUrl      VARCHAR(500) NULL COMMENT '专辑封面URL（网络）' AFTER coverUrl;
ALTER TABLE playlists ADD COLUMN coverNetworkUrl      VARCHAR(500) NULL COMMENT '歌单封面URL（网络）' AFTER coverUrl;
ALTER TABLE playlists ADD COLUMN backgroundNetworkUrl VARCHAR(500) NULL COMMENT '歌单背景图URL（网络）' AFTER backgroundUrl;

-- songs 表新增 lyrics 字段，存储本地音乐 LRC 格式歌词
ALTER TABLE songs ADD COLUMN lyrics LONGTEXT NULL COMMENT 'LRC格式歌词' AFTER fileUrl;
