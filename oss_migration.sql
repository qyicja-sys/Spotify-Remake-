-- =====================================================
-- OSS 迁移 SQL：将本地文件路径替换为 OSS URL
-- 执行前请先备份数据库！
-- OSS URL 前缀
-- =====================================================
SET @oss_base = 'https://java-ai.oss-cn-beijing.aliyuncs.com';

-- 1. songs 表 — 歌曲封面 & 音频文件
UPDATE songs
SET coverUrl = CONCAT(@oss_base, '/', SUBSTRING(coverUrl, 9))
WHERE coverUrl LIKE '/static/%';

UPDATE songs
SET fileUrl = CONCAT(@oss_base, '/', SUBSTRING(fileUrl, 9))
WHERE fileUrl LIKE '/static/%';

-- 2. artists 表 — 歌手头像
UPDATE artists
SET avatarUrl = CONCAT(@oss_base, '/', SUBSTRING(avatarUrl, 9))
WHERE avatarUrl LIKE '/static/%';

-- 3. playlists 表 — 歌单封面 & 背景图
UPDATE playlists
SET coverUrl = CONCAT(@oss_base, '/', SUBSTRING(coverUrl, 9))
WHERE coverUrl LIKE '/static/%';

UPDATE playlists
SET backgroundUrl = CONCAT(@oss_base, '/', SUBSTRING(backgroundUrl, 9))
WHERE backgroundUrl LIKE '/static/%';

-- 4. albums 表 — 专辑封面
UPDATE albums
SET coverUrl = CONCAT(@oss_base, '/', SUBSTRING(coverUrl, 9))
WHERE coverUrl LIKE '/static/%';

-- 5. user 表 — 用户头像
UPDATE user
SET profilePic = CONCAT(@oss_base, '/', SUBSTRING(profilePic, 9))
WHERE profilePic LIKE '/static/%';

-- =====================================================
-- 验证：查看还有多少条记录未迁移
-- =====================================================
SELECT 'songs.coverUrl' AS tbl, COUNT(*) AS remaining FROM songs WHERE coverUrl LIKE '/static/%'
UNION ALL
SELECT 'songs.fileUrl', COUNT(*) FROM songs WHERE fileUrl LIKE '/static/%'
UNION ALL
SELECT 'artists.avatarUrl', COUNT(*) FROM artists WHERE avatarUrl LIKE '/static/%'
UNION ALL
SELECT 'playlists.coverUrl', COUNT(*) FROM playlists WHERE coverUrl LIKE '/static/%'
UNION ALL
SELECT 'playlists.backgroundUrl', COUNT(*) FROM playlists WHERE backgroundUrl LIKE '/static/%'
UNION ALL
SELECT 'albums.coverUrl', COUNT(*) FROM albums WHERE coverUrl LIKE '/static/%'
UNION ALL
SELECT 'user.profilePic', COUNT(*) FROM user WHERE profilePic LIKE '/static/%';
