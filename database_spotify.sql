-- MySQL dump 10.13  Distrib 8.0.13, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: database_spotify
-- ------------------------------------------------------
-- Server version	8.0.13

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 SET NAMES utf8mb4 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `albums`
--

DROP TABLE IF EXISTS `albums`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `albums` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '专辑唯一ID',
  `artistId` bigint(20) unsigned NOT NULL COMMENT '创建该专辑的艺术家ID',
  `name` varchar(255) NOT NULL COMMENT '专辑名称',
  `coverUrl` varchar(500) DEFAULT NULL COMMENT '专辑封面URL',
  `coverNetworkUrl` varchar(500) DEFAULT NULL COMMENT '专辑封面URL（网络）',
  `description` text COMMENT '专辑简介/描述',
  `type` tinyint(4) NOT NULL DEFAULT '2' COMMENT '专辑类型：0-单曲Single，1-EP，2-专辑Album，3-精选集Compilation',
  `releaseDate` date DEFAULT NULL COMMENT '发行日期',
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_artist_id` (`artistId`),
  KEY `idx_release_date` (`releaseDate`),
  CONSTRAINT `fk_album_artist` FOREIGN KEY (`artistId`) REFERENCES `artists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专辑表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `artists`
--

DROP TABLE IF EXISTS `artists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `artists` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '歌手名字',
  `fansCount` bigint(20) unsigned DEFAULT '0' COMMENT '粉丝数量',
  `monthlyListeners` bigint(20) unsigned DEFAULT '0' COMMENT '每月听众数',
  `biography` text COMMENT '个人简介',
  `avatarUrl` varchar(500) DEFAULT NULL COMMENT '歌手头像URL',
  `avatarNetworkUrl` varchar(500) DEFAULT NULL COMMENT '歌手头像URL（网络）',
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playlist_songs`
--

DROP TABLE IF EXISTS `playlist_songs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `playlist_songs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `playlistId` bigint(20) unsigned NOT NULL COMMENT '歌单ID',
  `songId` bigint(20) unsigned NOT NULL COMMENT '歌曲ID',
  `addedAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_playlist_song` (`playlistId`,`songId`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `playlists`
--

DROP TABLE IF EXISTS `playlists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `playlists` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '歌单唯一ID',
  `userId` bigint(20) unsigned NOT NULL COMMENT '创建该歌单的用户ID',
  `profile` text COMMENT '歌单简介',
  `coverUrl` varchar(500) DEFAULT NULL COMMENT '专辑封面URL',
  `coverNetworkUrl` varchar(500) DEFAULT NULL COMMENT '歌单封面URL（网络）',
  `backgroundUrl` varchar(500) DEFAULT NULL COMMENT '歌单背景图URL',
  `backgroundNetworkUrl` varchar(500) DEFAULT NULL COMMENT '歌单背景图URL（网络）',
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `type` tinyint(4) DEFAULT '0' COMMENT '0:用户自建, 1:系统推荐, 2:官方精选',
  `name` varchar(255) NOT NULL DEFAULT 'Untitled Playlist',
  `is_private` tinyint(1) DEFAULT '0' COMMENT '是否私密',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `song_artist`
--

DROP TABLE IF EXISTS `song_artist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `song_artist` (
  `song_id` bigint(20) unsigned NOT NULL,
  `artist_id` bigint(20) unsigned NOT NULL,
  `is_main` tinyint(1) DEFAULT '1' COMMENT '1为主唱, 0为Feat/客串',
  PRIMARY KEY (`song_id`,`artist_id`),
  KEY `artist_id` (`artist_id`),
  CONSTRAINT `song_artist_ibfk_1` FOREIGN KEY (`song_id`) REFERENCES `songs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `song_artist_ibfk_2` FOREIGN KEY (`artist_id`) REFERENCES `artists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `song_play_log`
--

DROP TABLE IF EXISTS `song_play_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `song_play_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `song_id` int(11) NOT NULL,
  `play_count` bigint(20) unsigned DEFAULT '1',
  `play_date` date NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_song_date` (`song_id`,`play_date`),
  KEY `idx_play_date` (`play_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `songs`
--

DROP TABLE IF EXISTS `songs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `songs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `artistId` bigint(20) unsigned NOT NULL COMMENT '所属歌手ID',
  `albumId` bigint(20) unsigned DEFAULT NULL COMMENT '所属专辑ID，NULL表示单曲/未归入专辑',
  `title` varchar(255) NOT NULL COMMENT '歌曲名称',
  `coverUrl` varchar(500) DEFAULT NULL COMMENT '专辑封面URL',
  `coverNetworkUrl` varchar(500) DEFAULT NULL COMMENT '专辑封面URL（网络）',
  `fileUrl` varchar(500) NOT NULL COMMENT '音频文件路径',
  `lyrics` longtext COMMENT 'LRC格式歌词',
  `lyrics_url` varchar(500) DEFAULT NULL COMMENT '歌词URL（外部歌词地址）',
  `duration` int(10) unsigned DEFAULT NULL COMMENT '时长(秒)',
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `external_source` varchar(50) DEFAULT NULL,
  `external_id` varchar(200) DEFAULT NULL,
  `pic_id` varchar(100) DEFAULT NULL COMMENT '外部封面图片ID',
  PRIMARY KEY (`id`),
  KEY `idx_artist_id` (`artistId`),
  KEY `idx_album_id` (`albumId`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_admin`
--

DROP TABLE IF EXISTS `sys_admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `sys_admin` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号(唯一)',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密后的密码(如BCrypt加密)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删，1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统管理员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '序号',
  `userName` varchar(255) NOT NULL COMMENT '用户名',
  `nickName` varchar(255) NOT NULL COMMENT '昵称',
  `gender` varchar(4) DEFAULT '男' COMMENT '性别',
  `email` varchar(254) NOT NULL COMMENT '邮箱',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `personalMotto` text COMMENT '个性签名',
  `profilePic` varchar(255) DEFAULT 'static/datas/profilePic/default.jpg' COMMENT '用户头像图片路径',
  `is_artist` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为艺术家：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `userName` (`userName`),
  UNIQUE KEY `user_name` (`userName`),
  UNIQUE KEY `uk_user_username` (`userName`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_artist_follows`
--

DROP TABLE IF EXISTS `user_artist_follows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `user_artist_follows` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `artist_id` bigint(20) unsigned NOT NULL COMMENT '艺人ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_artist` (`user_id`,`artist_id`),
  KEY `idx_artist_user` (`artist_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注艺人关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_collected_playlists`
--

DROP TABLE IF EXISTS `user_collected_playlists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `user_collected_playlists` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) unsigned NOT NULL COMMENT '收藏者用户ID',
  `playlistId` bigint(20) unsigned NOT NULL COMMENT '被收藏的歌单ID',
  `collectedAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_playlist` (`userId`,`playlistId`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏歌单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_playback_history`
--

DROP TABLE IF EXISTS `user_playback_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `user_playback_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键，单表容量大，使用BIGINT',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `song_id` bigint(20) unsigned NOT NULL COMMENT '歌曲ID',
  `artist_id` bigint(20) unsigned NOT NULL COMMENT '艺人/歌手ID，冗余该字段便于直接统计热门艺人',
  `played_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '听歌时间，默认当前时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`played_at`),
  KEY `idx_artist_id` (`artist_id`)
) ENGINE=InnoDB AUTO_INCREMENT=294 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户听歌历史流水表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-02 22:05:37
