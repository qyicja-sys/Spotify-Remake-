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
-- Dumping data for table `albums`
--

LOCK TABLES `albums` WRITE;
/*!40000 ALTER TABLE `albums` DISABLE KEYS */;
INSERT INTO `albums` VALUES (1,2,'create your love','/static/datas/musicResouces/music_cover/albums/create your love.jpg',NULL,'create your love',0,'2026-06-20','2026-06-20 10:10:37'),(2,2,'eloquence,my dear','/static/datas/musicResouces/music_cover/albums/eloquence,my dear.jpg',NULL,'eloquence,my dear',0,'2026-06-20','2026-06-20 10:22:54'),(3,2,'Haute Couture','/static/datas/musicResouces/music_cover/albums/Haute Couture.jpg',NULL,'Haute Couture',2,'2026-06-20','2026-06-20 10:56:08'),(4,2,'princess hour','/static/datas/musicResouces/music_cover/albums/princess hour.jpg',NULL,'princess hour',0,'2026-06-20','2026-06-20 10:57:51'),(5,2,'Velvet','/static/datas/musicResouces/music_cover/albums/Velvet.jpg',NULL,'Velvet',2,'2026-06-20','2026-06-20 11:00:43');
/*!40000 ALTER TABLE `albums` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `artists`
--

LOCK TABLES `artists` WRITE;
/*!40000 ALTER TABLE `artists` DISABLE KEYS */;
INSERT INTO `artists` VALUES (1,'pooka',NULL,NULL,'','/static/datas/profilePic/artists/pooka.jpg','https://i.scdn.co/image/ab6761610000101f591f00de729b736386d22ea9','2026-05-31 10:25:19'),(2,'datealyfe',NULL,NULL,'','/static/datas/profilePic/artists/datealyfe.jpg','https://i.scdn.co/image/ab6761610000e5ebd79fb7d05f0717c3b8cd2915','2026-06-06 08:42:04'),(3,'mimichi',NULL,NULL,'','/static/datas/profilePic/artists/mimichi.jpg','https://i.scdn.co/image/ab676161000086f737aff7c832e6eda310560f5a','2026-06-06 08:42:04'),(4,'ArcticMonkeys',NULL,NULL,NULL,'/static/datas/profilePic/artists/ArcticMonkeys_muisic.jpg','https://i.scdn.co/image/ab6761670000ecd4c35db22d2c52ffa9bc8d5684','2026-06-10 13:59:54'),(5,'Mr.Kitty, PASTEL GHOST',NULL,NULL,NULL,NULL,NULL,'2026-06-25 07:49:28'),(6,'EsDeeKid, Rico Ace',NULL,NULL,NULL,NULL,NULL,'2026-06-25 09:07:23'),(7,'EsDeeKid, fakemink, Rico Ace',NULL,NULL,NULL,NULL,NULL,'2026-06-25 09:11:58'),(8,'Alan Walker',NULL,NULL,NULL,NULL,NULL,'2026-06-25 09:19:09'),(9,'EsDeeKid',NULL,NULL,NULL,NULL,NULL,'2026-06-25 09:19:28'),(10,'Rico Ace',NULL,NULL,NULL,NULL,NULL,'2026-06-25 09:36:54'),(11,'fakemink',NULL,NULL,NULL,NULL,NULL,'2026-06-25 10:09:14'),(12,'Coolie Boii',NULL,NULL,NULL,NULL,NULL,'2026-06-25 11:31:13'),(13,'Lucy Bedroque',NULL,NULL,NULL,NULL,NULL,'2026-06-26 03:33:37'),(14,'周杰伦',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(15,'李硕',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(16,'张鑫',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(17,'温岚',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(18,'吴宗宪',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(19,'蔡依林',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(20,'那英',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(21,'李荣浩',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(22,'李玟',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(23,'宿涵',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(24,'张神儿',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(25,'刘欢',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:12'),(26,'陈颖恩',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:19'),(27,'肖邦妮',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:19'),(28,'朱文婷',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:19'),(29,'汪峰',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:19'),(30,'庾澄庆',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:19'),(31,'Ricii Lompeurs',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(32,'Ticia',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(33,'KKK',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(34,'bai',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(35,'Sail',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(36,'IRXD7',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(37,'Coldplay',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(38,'BAND-MAID',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(39,'安崎',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(40,'葛鑫怡',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(41,'黄欣苑',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(42,'金子涵',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(43,'孔雪儿',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(44,'魏辰',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(45,'文哲',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(46,'金请夏',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(47,'CHANGMO',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:52'),(48,'K-391',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:53'),(49,'Tungevaag',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:53'),(50,'Mangoo',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:53:53'),(51,'Playboi Carti',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:05'),(52,'Travis Scott',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:05'),(53,'The Weeknd',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:05'),(54,'Madonna',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:05'),(55,'赵水水',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:15'),(56,'Gunna',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:15'),(57,'Doechii',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:15'),(58,'Quality Control',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:15'),(59,'Offset',NULL,NULL,NULL,NULL,NULL,'2026-06-26 07:54:15'),(60,'林湫杰',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(61,'晴天1',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(62,'蒋倩如',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(63,'陈玉婷',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(64,'卡其小男孩',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(65,'吴海啸Tsunami',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(66,'Young 7',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:07'),(67,'Lucky小爱',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(68,'GYBeat',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(69,'RyaVocal',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(70,'平野綾',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(71,'茅原実里',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(72,'後藤邑子',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(73,'DJ阿罗',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(74,'Morfonica',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(75,'周杰伦.',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(76,'Asasblue',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(77,'梦里啥都有',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(78,'PENGUIN RESEARCH',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(79,'余不不',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:10'),(80,'SEKAI NO OWARI',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(81,'Mr.Kitty',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(82,'PASTEL GHOST',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(83,'Farruko',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(84,'Sofia Reyes',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(85,'三角洲行动',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(86,'Rain Man',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(87,'T-Mass',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(88,'Krysta Youngs',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(89,'Tove Lo',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(90,'篠原侑',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(91,'I.M',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(92,'egobreak',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(93,'prettifun',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(94,'Scott Bradlee’s Postmodern Jukebox',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(95,'Haley Reinhart',NULL,NULL,NULL,NULL,NULL,'2026-06-26 16:49:21'),(96,'Jonasu',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:48:00'),(97,'Felix Samuel',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:48:00'),(98,'Maria Mena',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:48:00'),(99,'是一条深情鱼',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:48:00'),(100,'小狗',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:48:00'),(101,'molly',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(102,'Anti-Light Archive',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(103,'Hini',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(104,'Seph',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(105,'Rimeren',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(106,'rumae',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(107,'7undra',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(108,'coldspades',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(109,'Knives',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(110,'2adore',NULL,NULL,NULL,NULL,NULL,'2026-06-27 07:54:55'),(111,'sayako',NULL,NULL,NULL,NULL,NULL,'2026-06-27 12:29:35'),(112,'finbow',NULL,NULL,NULL,NULL,NULL,'2026-06-27 12:29:35'),(113,'Sheap',NULL,NULL,NULL,NULL,NULL,'2026-06-27 12:29:35');
/*!40000 ALTER TABLE `artists` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `playlist_songs`
--

LOCK TABLES `playlist_songs` WRITE;
/*!40000 ALTER TABLE `playlist_songs` DISABLE KEYS */;
INSERT INTO `playlist_songs` VALUES (1,1,5,'2026-06-06 10:43:23'),(2,1,4,'2026-06-06 10:43:25'),(3,1,3,'2026-06-06 10:43:26'),(4,1,2,'2026-06-06 10:43:29'),(7,9,3,'2026-06-17 06:47:13'),(8,9,4,'2026-06-17 06:47:14'),(12,9,5,'2026-06-17 07:33:57'),(13,23,2,'2026-06-17 07:45:24'),(15,9,2,'2026-06-23 13:02:07'),(16,13,5,'2026-06-24 08:56:52'),(17,9,47,'2026-06-25 15:07:22'),(18,9,9,'2026-06-26 02:39:35'),(19,9,49,'2026-06-26 03:05:18'),(20,9,52,'2026-06-26 03:43:04');
/*!40000 ALTER TABLE `playlist_songs` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `playlists`
--

LOCK TABLES `playlists` WRITE;
/*!40000 ALTER TABLE `playlists` DISABLE KEYS */;
INSERT INTO `playlists` VALUES (1,1,'datealyfe和mimichi','/static/datas/musicResouces/music_cover/daliy/sys_daily_1.png',NULL,NULL,NULL,'2026-05-28 05:16:39',1,'每日推荐1',0),(2,1,'wishlane、ericdoa、Aries等更多曲风','/static/datas/musicResouces/music_cover/daliy/sys_daily_2.png',NULL,NULL,NULL,'2026-05-28 05:25:56',1,'每日推荐2',0),(3,1,'Bazzi、The Vamps、Taylor Swift等更多曲风','/static/datas/musicResouces/music_cover/daliy/sys_daily_3.png',NULL,NULL,NULL,'2026-05-28 05:25:56',1,'每日推荐3',0),(4,1,'d4vd,Vansire、girl in Red等更多曲风','/static/datas/musicResouces/music_cover/daliy/sys_daily_4.png',NULL,NULL,NULL,'2026-05-28 05:25:56',1,'每日推荐4',0),(5,1,'LE SSERAFIM和NewJeans','/static/datas/musicResouces/music_cover/daliy/sys_daily_5.png',NULL,NULL,NULL,'2026-05-28 05:25:56',1,'每日推荐5',0),(6,1,'NewJeans和LESSERAFIM','/static/datas/musicResouces/music_cover/daliy/sys_daily_6.png',NULL,NULL,NULL,'2026-05-28 05:25:56',1,'每日推荐6',0),(7,1,'精选小众宝藏音乐、冷门佳作和未来金曲，每周...','/static/datas/musicResouces/music_cover/daliy/sys_weekly_find.png',NULL,'/static/datas/musicResouces/music_cover/daliy/background/sys_weekly_find_bg.jpg',NULL,'2026-05-28 05:25:56',1,'每周新发现',0),(8,1,'捕捉你关注的艺人的最新音乐，以及为你推荐的...','/static/datas/musicResouces/music_cover/daliy/sys_newMusicRadar.png',NULL,NULL,NULL,'2026-05-28 05:25:56',1,'新歌雷达',0),(9,2,'已点赞的歌曲','/static/datas/musicResouces/music_cover/personal/like_songs.png',NULL,NULL,NULL,'2026-05-28 06:55:51',0,'已点赞的歌曲',0),(10,3,'已点赞的歌曲','/static/datas/musicResouces/music_cover/personal/like_songs.png',NULL,NULL,NULL,'2026-05-28 07:16:06',0,'已点赞的歌曲',0),(12,6,NULL,'/static/datas/musicResouces/music_cover/personal/like_songs.png',NULL,NULL,NULL,'2026-06-06 08:07:38',0,'已点赞的歌曲',0),(13,7,NULL,'/static/datas/musicResouces/music_cover/personal/like_songs.png',NULL,NULL,NULL,'2026-06-06 08:09:06',0,'已点赞的歌曲',0),(14,8,NULL,'/static/datas/musicResouces/music_cover/personal/like_songs.png',NULL,NULL,NULL,'2026-06-06 08:10:06',0,'已点赞的歌曲',0),(21,9,NULL,'/static/datas/musicResouces/music_cover/personal/like_songs.png',NULL,NULL,NULL,'2026-06-10 13:56:52',0,'已点赞的歌曲',0),(23,2,'经常爱听的音乐','/static/datas/musicResouces/music_cover/personal/2_23.jpg',NULL,NULL,NULL,'2026-06-16 15:15:06',0,'常听',0);
/*!40000 ALTER TABLE `playlists` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `song_artist`
--

LOCK TABLES `song_artist` WRITE;
/*!40000 ALTER TABLE `song_artist` DISABLE KEYS */;
INSERT INTO `song_artist` VALUES (1,1,1),(2,2,1),(2,3,0),(3,2,0),(3,3,1),(4,2,0),(4,3,1),(5,2,0),(5,3,1),(6,4,1),(8,2,1),(9,2,1),(10,2,1),(11,2,1),(12,2,1),(13,2,1),(14,2,1),(15,2,1),(16,2,1),(17,2,1),(18,2,1),(19,2,1),(47,9,1),(47,10,0),(48,9,1),(49,9,1),(50,9,1),(50,10,0),(50,11,0),(52,13,1),(53,51,0),(53,52,1),(54,51,1),(54,53,0),(55,81,1),(55,82,0);
/*!40000 ALTER TABLE `song_artist` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `songs`
--

LOCK TABLES `songs` WRITE;
/*!40000 ALTER TABLE `songs` DISABLE KEYS */;
INSERT INTO `songs` VALUES (1,1,NULL,'5ORRYBOU2DA2','/static/datas/musicResouces/music_cover/songs/pooka - 5ORRYBOU2DA2.jpg',NULL,'/static/datas/musicResouces/musics/pooka - 5ORRYBOU2DA2.mp3',NULL,NULL,111,'2026-05-31 10:30:53',NULL,NULL,NULL),(2,2,NULL,'eat you up','/static/datas/musicResouces/music_cover/songs/datealyfe - eat you up.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - eat you up.mp3',NULL,NULL,132,'2026-06-06 09:10:10',NULL,NULL,NULL),(3,3,NULL,'for my strawberry','/static/datas/musicResouces/music_cover/songs/mimichi - for my strawberry.jpg',NULL,'/static/datas/musicResouces/musics/mimichi - for my strawberry.mp3',NULL,NULL,122,'2026-06-06 09:10:47',NULL,NULL,NULL),(4,3,NULL,'i want to talk about you','/static/datas/musicResouces/music_cover/songs/mimichi - i want to talk about you.jpg',NULL,'/static/datas/musicResouces/musics/mimichi - i want to talk about you.mp3',NULL,NULL,111,'2026-06-06 09:11:46',NULL,NULL,NULL),(5,3,NULL,'tiramisu','/static/datas/musicResouces/music_cover/songs/mimichi - tiramisu.jpg',NULL,'/static/datas/musicResouces/musics/mimichi - tiramisu.mp3',NULL,NULL,122,'2026-06-06 09:12:25',NULL,NULL,NULL),(6,4,NULL,'No. 1 Party Anthem','/static/datas/musicResouces/music_cover/songs/ArcticMonkeys - No. 1 Party Anthem.jpg',NULL,'/static/datas/musicResouces/musics/ArcticMonkeys - No. 1 Party Anthem.mp3',NULL,NULL,243,'2026-06-10 14:24:40',NULL,NULL,NULL),(8,2,1,'create your love','/static/datas/musicResouces/music_cover/albums/create your love.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - create your love ᰔᩚ.mp3',NULL,NULL,138,'2026-06-20 10:10:37',NULL,NULL,NULL),(9,2,2,'eloquence,my dear','/static/datas/musicResouces/music_cover/albums/eloquence,my dear.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - eloquence,my dear.mp3',NULL,NULL,138,'2026-06-20 10:22:54',NULL,NULL,NULL),(10,2,3,'butterfly ring 𓏲ּ𝄢','/static/datas/musicResouces/music_cover/albums/Haute Couture.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - butterfly ring 𓏲ּ𝄢.mp3',NULL,NULL,132,'2026-06-20 10:56:08',NULL,NULL,NULL),(11,2,3,'easy to tell ⋆˚✿˖°','/static/datas/musicResouces/music_cover/albums/Haute Couture.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - easy to tell ⋆˚✿˖°.mp3',NULL,NULL,104,'2026-06-20 10:56:08',NULL,NULL,NULL),(12,2,3,'in love ft sayako ♡✧˚ ༘','/static/datas/musicResouces/music_cover/albums/Haute Couture.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - in love ft sayako ♡✧˚ ༘.mp3',NULL,NULL,95,'2026-06-20 10:56:08',NULL,NULL,NULL),(13,2,3,'please put them on ˗ˋˏ ♡ ˎˊ˗','/static/datas/musicResouces/music_cover/albums/Haute Couture.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - please put them on ˗ˋˏ ♡ ˎˊ˗.mp3',NULL,NULL,103,'2026-06-20 10:56:08',NULL,NULL,NULL),(14,2,3,'tongue tied 𐙚˙⋆.˚ ᡣ𐭩','/static/datas/musicResouces/music_cover/albums/Haute Couture.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - tongue tied 𐙚˙⋆.˚ ᡣ𐭩.mp3',NULL,NULL,124,'2026-06-20 10:56:08',NULL,NULL,NULL),(15,2,4,'princess hour °❀⋆.ೃ࿔･','/static/datas/musicResouces/music_cover/albums/princess hour.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - princess hour °❀⋆.ೃ࿔･.mp3',NULL,NULL,126,'2026-06-20 10:57:51',NULL,NULL,NULL),(16,2,5,'Flower girl 𓍢ִ໋❀˚⋆','/static/datas/musicResouces/music_cover/albums/Velvet.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - Flower girl 𓍢ִ໋❀˚⋆.mp3',NULL,NULL,108,'2026-06-20 11:00:43',NULL,NULL,NULL),(17,2,5,'Next to me જ⁀➴ ♡','/static/datas/musicResouces/music_cover/albums/Velvet.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - Next to me જ⁀➴ ♡.mp3',NULL,NULL,129,'2026-06-20 11:00:43',NULL,NULL,NULL),(18,2,5,'Special doll 𓊆ྀི❤︎𓊇ྀ','/static/datas/musicResouces/music_cover/albums/Velvet.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - Special doll 𓊆ྀི❤︎𓊇ྀ.mp3',NULL,NULL,110,'2026-06-20 11:00:43',NULL,NULL,NULL),(19,2,5,'You\'re so vile (•́ ᴖ •̀)','/static/datas/musicResouces/music_cover/albums/Velvet.jpg',NULL,'/static/datas/musicResouces/musics/datealyfe - You\'re so vile (•́ ᴖ •̀).mp3',NULL,NULL,113,'2026-06-20 11:00:43',NULL,NULL,NULL),(47,9,NULL,'Cali Man',NULL,'https://p2.music.126.net/ya_Maz6byh9GQzyHA6YGYw==/109951171319034097.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-25 12:46:30','gdmusic','netease_2716372739','109951171319034097'),(48,9,NULL,'Panic',NULL,'https://p2.music.126.net/ya_Maz6byh9GQzyHA6YGYw==/109951171319034097.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-26 03:05:11','gdmusic','netease_2716372741',NULL),(49,9,NULL,'Prague',NULL,'https://p2.music.126.net/ya_Maz6byh9GQzyHA6YGYw==/109951171319034097.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-26 03:05:16','gdmusic','netease_2716374346',NULL),(50,9,NULL,'LV Sandals',NULL,'https://p2.music.126.net/ya_Maz6byh9GQzyHA6YGYw==/109951171319034097.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-26 03:06:34','gdmusic','netease_2716372740',NULL),(52,13,NULL,'How to Pretend',NULL,'https://p2.music.126.net/EmxtaIrXOSQE7nYzLVAHmw==/109951173264523781.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-26 03:33:37','gdmusic','netease_3385121011','109951173264523781'),(53,52,NULL,'FE!N',NULL,'https://p2.music.126.net/5Ax6aNOMn-mwF6IwYWJKMg==/109951168771978885.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-26 07:54:17','gdmusic','netease_2068069291',NULL),(54,51,NULL,'RATHER LIE',NULL,'https://p2.music.126.net/wpPyg5lOIVzh7bMxHPvYtg==/109951170611393931.jpg?param=300y300','',NULL,NULL,NULL,'2026-06-26 07:54:28','gdmusic','netease_2684616200',NULL),(55,81,NULL,'Habits',NULL,'https://p2.music.126.net/KausrSmDfrypbMbw587SXQ==/109951163343414953.jpg?param=300y300','','[00:00.000] 作词 : PASTEL GHOST\n[00:01.000] 作曲 : PASTEL GHOST/Mr.Kitty\n[00:02.000] 编曲 : PASTEL GHOST/Mr.Kitty\n[00:27.375]At night I travel my nerves unravel\n[00:34.691]It hurts to hide myself from you\n[00:41.715]With care I held it, inside I felt it\n[00:48.483]My favorite pain comes back to me\n[00:54.936]Don\'t break me, don\'t let go\n[01:08.556]Don\'t try to leave, let your habits control you\n[01:22.637]These blades I\'m hiding, keep safe the tidings\n[01:29.383]Of bridges crossed that soon collapsed\n[01:36.417]No fear of falling, endlessly calling\n[01:43.183]You make me feel like I\'m alone\n[01:49.938]Don\'t break me, don\'t let go\n[02:03.468]Don\'t try to leave, let your habits control you\n[02:44.819]Don\'t break me, don\'t let go\n[02:58.324]Don\'t try to leave, let your habits control you\n[03:12.102]Don\'t break me, don\'t let go\n[03:25.567]Don\'t try to leave, let your habits control you\n[03:39.360]Don\'t break me, don\'t let go\n[03:53.135]Don\'t try to leave, let your habits control you',NULL,NULL,'2026-06-26 16:49:22','gdmusic','netease_464035388','109951163343414953');
/*!40000 ALTER TABLE `songs` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `sys_admin`
--

LOCK TABLES `sys_admin` WRITE;
/*!40000 ALTER TABLE `sys_admin` DISABLE KEYS */;
INSERT INTO `sys_admin` VALUES (2,'cxy_666','$2b$12$MAwcCj1151VA/0UJEPkReeb5aj8hnrmYnBnI.nkWGDxHMK6Wkjt9W',1,NULL,'2026-05-31 16:01:26','2026-05-31 16:01:26',0);
/*!40000 ALTER TABLE `sys_admin` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'system','system','男','system','system',NULL,NULL,'/static/datas/profilePic/default.jpg',0),(2,'ty1l_aep','t1y','男','3531185223@qq.com','$2a$10$EXXu9y4Wmkr7zQbkcqpfieO1hDMxxqRKS3iepURB6tpAKZvkFmqeG',NULL,NULL,'/static/datas/profilePic/ty1l_aep.jpg',0),(3,'izxa_aep','izy',NULL,'qyicja@163.com','$2a$10$MXTtXVixtc72Dyav0HIij.6HwZ9C2ZSO8BSEslChPpoKao50u1m5y',NULL,NULL,'/static/datas/profilePic/default.jpg',0),(6,'pooka_music','pooka',NULL,'pooka@gmail.com','$2a$10$Bk0sMIA8SeEbmHgoBbuLPuXRrb7aEghvJU8nUP3A26PvZPbx/a74m',NULL,NULL,'/static/datas/profilePic/pooka.jpg',1),(7,'datealyfe_music','datealyfe',NULL,'datealyfe@gmail.com','$2a$10$G7Zqrll34nmg8AyX72mSOe3vzUoFnsYB2fQ/mKVYeyFPlsnBYyfCG',NULL,NULL,'/static/datas/profilePic/datealyfe.jpg',1),(8,'mimichi_music','mimichi',NULL,'mimichi@gmail.com','$2a$10$cbxcQ4/CMsECCZqmWsvlO.5bquWJn5uMXlNpLRay7G1Ao5Nwjlrna',NULL,NULL,'/static/datas/profilePic/mimichi.jpg',1),(9,'ArcticMonkeys_muisic','ArcticMonkeys',NULL,'ArcticMonkeys@gmail.com','$2a$10$qGSt86ytaJSZtL1F6IwREOWlTD6yfkYb40WKp.eeqGBx2WdLHikPW',NULL,NULL,'/static/datas/profilePic/ArcticMonkeys_muisic.jpg',1);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `user_artist_follows`
--

LOCK TABLES `user_artist_follows` WRITE;
/*!40000 ALTER TABLE `user_artist_follows` DISABLE KEYS */;
INSERT INTO `user_artist_follows` VALUES (2,2,2,'2026-06-21 12:51:48');
/*!40000 ALTER TABLE `user_artist_follows` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `user_collected_playlists`
--

LOCK TABLES `user_collected_playlists` WRITE;
/*!40000 ALTER TABLE `user_collected_playlists` DISABLE KEYS */;
INSERT INTO `user_collected_playlists` VALUES (3,2,1,'2026-06-19 07:47:20');
/*!40000 ALTER TABLE `user_collected_playlists` ENABLE KEYS */;
UNLOCK TABLES;

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
) ENGINE=InnoDB AUTO_INCREMENT=285 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户听歌历史流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_playback_history`
--

LOCK TABLES `user_playback_history` WRITE;
/*!40000 ALTER TABLE `user_playback_history` DISABLE KEYS */;
INSERT INTO `user_playback_history` VALUES (1,2,2,2,'2026-06-09 21:32:20'),(2,2,2,2,'2026-06-10 13:32:00'),(3,2,2,2,'2026-06-10 19:46:12'),(4,2,5,3,'2026-06-10 19:52:41'),(5,2,1,1,'2026-06-10 19:53:20'),(6,9,5,3,'2026-06-10 22:51:07'),(7,2,2,2,'2026-06-15 22:22:08'),(8,2,5,3,'2026-06-15 22:35:22'),(9,2,5,3,'2026-06-15 23:17:03'),(10,2,5,3,'2026-06-17 14:47:06'),(11,2,4,3,'2026-06-17 14:47:09'),(12,2,2,2,'2026-06-17 14:47:11'),(13,2,5,3,'2026-06-17 15:27:04'),(14,2,2,2,'2026-06-17 16:26:18'),(15,2,2,2,'2026-06-17 16:33:59'),(16,2,5,3,'2026-06-17 16:34:02'),(17,2,4,3,'2026-06-17 16:34:03'),(18,2,1,1,'2026-06-17 16:34:04'),(19,2,2,2,'2026-06-19 15:09:28'),(20,2,2,2,'2026-06-19 15:09:35'),(21,2,5,3,'2026-06-19 15:46:09'),(22,9,6,4,'2026-06-19 21:27:07'),(23,8,5,3,'2026-06-19 21:38:05'),(24,8,4,3,'2026-06-19 21:38:06'),(25,8,3,3,'2026-06-19 21:38:08'),(26,8,2,2,'2026-06-19 21:38:08'),(27,2,5,3,'2026-06-20 16:26:25'),(28,2,5,3,'2026-06-20 16:26:40'),(29,7,2,2,'2026-06-20 18:37:19'),(30,7,9,2,'2026-06-20 18:40:56'),(31,7,8,2,'2026-06-20 18:41:01'),(32,7,4,3,'2026-06-20 18:41:03'),(33,7,8,2,'2026-06-20 18:41:04'),(34,7,9,2,'2026-06-20 18:42:46'),(35,7,8,2,'2026-06-20 18:42:49'),(36,7,10,2,'2026-06-20 18:56:38'),(37,7,15,2,'2026-06-20 18:57:53'),(38,7,16,2,'2026-06-20 19:44:56'),(39,7,17,2,'2026-06-20 19:44:59'),(40,7,15,2,'2026-06-20 19:45:07'),(41,7,16,2,'2026-06-21 18:11:59'),(42,7,16,2,'2026-06-21 19:46:38'),(43,2,5,3,'2026-06-21 21:00:12'),(44,2,5,3,'2026-06-21 21:00:12'),(45,2,16,2,'2026-06-23 15:37:31'),(46,2,17,2,'2026-06-23 15:37:41'),(47,2,16,2,'2026-06-23 15:37:44'),(48,2,11,2,'2026-06-23 15:38:42'),(49,2,16,2,'2026-06-23 15:53:55'),(50,2,17,2,'2026-06-23 15:55:43'),(51,2,5,3,'2026-06-23 16:00:49'),(52,2,5,3,'2026-06-23 16:08:47'),(53,2,5,3,'2026-06-23 16:24:26'),(54,7,5,3,'2026-06-23 16:55:06'),(55,2,5,3,'2026-06-23 16:55:26'),(56,7,5,3,'2026-06-23 17:00:07'),(57,7,5,3,'2026-06-23 17:03:09'),(58,2,5,3,'2026-06-23 18:17:05'),(59,7,5,3,'2026-06-23 20:13:03'),(60,2,4,3,'2026-06-23 20:13:23'),(61,2,2,2,'2026-06-23 20:13:31'),(62,7,5,3,'2026-06-23 20:13:36'),(63,7,4,3,'2026-06-23 20:13:42'),(64,7,4,3,'2026-06-23 20:13:52'),(65,7,5,3,'2026-06-23 20:13:53'),(66,2,2,2,'2026-06-23 21:02:10'),(67,2,2,2,'2026-06-23 21:06:51'),(68,2,2,2,'2026-06-23 21:07:08'),(69,7,5,3,'2026-06-23 21:09:11'),(70,2,2,2,'2026-06-23 21:20:57'),(71,2,4,3,'2026-06-23 21:21:02'),(72,2,5,3,'2026-06-23 21:21:25'),(73,2,2,2,'2026-06-23 22:00:03'),(74,2,2,2,'2026-06-23 22:47:31'),(75,2,5,3,'2026-06-23 22:49:44'),(76,7,5,3,'2026-06-24 16:56:09'),(77,7,4,3,'2026-06-24 16:56:15'),(78,7,3,3,'2026-06-24 16:56:18'),(79,7,2,2,'2026-06-24 16:56:19'),(80,7,5,3,'2026-06-24 16:56:20'),(81,7,5,3,'2026-06-24 19:23:04'),(82,7,5,3,'2026-06-24 19:23:10'),(83,7,5,3,'2026-06-24 22:31:18'),(84,7,5,3,'2026-06-25 14:21:08'),(85,2,5,3,'2026-06-25 15:22:50'),(88,2,12,2,'2026-06-25 16:51:30'),(89,2,14,2,'2026-06-25 16:51:33'),(193,2,47,9,'2026-06-25 20:46:30'),(194,2,47,10,'2026-06-25 20:46:30'),(195,2,47,9,'2026-06-25 20:46:57'),(196,2,47,10,'2026-06-25 20:46:57'),(197,2,2,2,'2026-06-25 20:47:50'),(198,2,9,2,'2026-06-25 21:02:48'),(199,2,9,2,'2026-06-25 22:46:11'),(200,2,47,9,'2026-06-25 22:46:21'),(201,2,47,10,'2026-06-25 22:46:21'),(202,2,47,9,'2026-06-25 22:46:28'),(203,2,47,10,'2026-06-25 22:46:28'),(204,2,47,9,'2026-06-25 23:04:08'),(205,2,47,10,'2026-06-25 23:04:08'),(206,2,47,9,'2026-06-25 23:06:31'),(207,2,47,10,'2026-06-25 23:06:32'),(208,2,9,2,'2026-06-26 10:39:23'),(209,2,47,9,'2026-06-26 10:50:01'),(210,2,47,10,'2026-06-26 10:50:01'),(211,2,47,9,'2026-06-26 10:54:58'),(212,2,47,10,'2026-06-26 10:54:58'),(213,2,47,9,'2026-06-26 10:54:59'),(214,2,47,10,'2026-06-26 10:54:59'),(215,2,47,9,'2026-06-26 10:55:17'),(216,2,47,10,'2026-06-26 10:55:17'),(217,2,3,2,'2026-06-26 11:04:55'),(218,2,3,3,'2026-06-26 11:04:55'),(219,2,47,9,'2026-06-26 11:04:59'),(220,2,47,10,'2026-06-26 11:04:59'),(221,2,48,9,'2026-06-26 11:05:12'),(222,2,49,9,'2026-06-26 11:05:16'),(223,2,50,9,'2026-06-26 11:06:35'),(224,2,50,11,'2026-06-26 11:06:35'),(225,2,50,10,'2026-06-26 11:06:35'),(226,2,47,9,'2026-06-26 11:21:13'),(227,2,47,10,'2026-06-26 11:21:13'),(228,2,51,3,'2026-06-26 11:24:49'),(229,2,51,2,'2026-06-26 11:24:49'),(230,2,4,2,'2026-06-26 11:33:08'),(231,2,4,3,'2026-06-26 11:33:08'),(232,2,52,13,'2026-06-26 11:33:38'),(233,2,51,2,'2026-06-26 11:48:52'),(234,2,51,3,'2026-06-26 11:48:52'),(235,2,16,2,'2026-06-26 11:48:58'),(236,2,2,2,'2026-06-26 11:49:06'),(237,2,2,3,'2026-06-26 11:49:06'),(238,2,50,9,'2026-06-26 11:56:06'),(239,2,50,11,'2026-06-26 11:56:06'),(240,2,50,10,'2026-06-26 11:56:06'),(241,2,2,2,'2026-06-26 11:56:31'),(242,2,2,3,'2026-06-26 11:56:31'),(243,2,47,9,'2026-06-26 15:17:44'),(244,2,47,10,'2026-06-26 15:17:44'),(245,2,53,52,'2026-06-26 15:54:18'),(246,2,53,51,'2026-06-26 15:54:18'),(247,2,54,51,'2026-06-26 15:54:29'),(248,2,54,53,'2026-06-26 15:54:29'),(249,2,4,2,'2026-06-26 22:27:29'),(250,2,4,3,'2026-06-26 22:27:29'),(251,2,4,2,'2026-06-27 00:43:08'),(252,2,4,3,'2026-06-27 00:43:09'),(253,2,55,81,'2026-06-27 00:49:23'),(254,2,55,82,'2026-06-27 00:49:23'),(255,2,9,2,'2026-06-27 15:39:32'),(256,2,5,2,'2026-06-27 15:40:03'),(257,2,5,3,'2026-06-27 15:40:03'),(258,2,47,9,'2026-06-27 15:40:17'),(259,2,47,10,'2026-06-27 15:40:17'),(260,2,55,81,'2026-06-27 15:49:00'),(261,2,55,82,'2026-06-27 15:49:00'),(262,2,16,2,'2026-06-27 15:54:54'),(263,2,47,9,'2026-06-27 15:54:59'),(264,2,47,10,'2026-06-27 15:54:59'),(265,2,3,2,'2026-06-27 16:30:32'),(266,2,3,3,'2026-06-27 16:30:33'),(267,2,3,2,'2026-06-27 16:37:12'),(268,2,3,3,'2026-06-27 16:37:12'),(269,2,2,2,'2026-06-27 16:37:27'),(270,2,2,3,'2026-06-27 16:37:28'),(271,2,52,13,'2026-06-27 16:37:33'),(272,2,47,9,'2026-06-27 17:32:58'),(273,2,47,10,'2026-06-27 17:32:58'),(274,2,2,2,'2026-06-27 19:03:14'),(275,2,2,3,'2026-06-27 19:03:14'),(276,2,47,9,'2026-06-27 19:05:27'),(277,2,47,10,'2026-06-27 19:05:27'),(278,2,2,2,'2026-06-27 19:08:53'),(279,2,2,3,'2026-06-27 19:08:53'),(280,2,47,9,'2026-06-27 19:09:00'),(281,2,47,10,'2026-06-27 19:09:00'),(282,2,9,2,'2026-06-27 19:10:50'),(283,2,49,9,'2026-06-27 19:11:00'),(284,2,52,13,'2026-06-27 20:28:35');
/*!40000 ALTER TABLE `user_playback_history` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-27 22:15:15
