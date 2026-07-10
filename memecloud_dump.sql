-- ============================================================
-- MemeCloud Database Dump
-- 生成日期: 2026-07-10 15:17
-- MySQL 8.0, utf8mb4
--
-- 用法:
--   mysql -u root -p < memecloud_dump.sql
--   或
--   mysql -u root -p --default-character-set=utf8mb4 < memecloud_dump.sql
--
-- 测试账号: testuser / 123456
-- ============================================================
mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: memecloud
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `memecloud`
--

/*!40000 DROP DATABASE IF EXISTS `memecloud`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `memecloud` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `memecloud`;

--
-- Table structure for table `bounties`
--

DROP TABLE IF EXISTS `bounties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bounties` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '悬赏ID',
  `publisher_id` bigint NOT NULL COMMENT '发布者',
  `title` varchar(100) NOT NULL COMMENT '悬赏标题',
  `description` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `points_reward` int NOT NULL COMMENT '悬赏积分（发布时扣除）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=进行中, 1=已采纳, 2=已关闭',
  `accepted_user_id` bigint DEFAULT NULL COMMENT '被采纳者',
  `accepted_emoji_id` bigint DEFAULT NULL COMMENT '被采纳的表情包',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `fk_bounties_accepted_user` (`accepted_user_id`),
  KEY `fk_bounties_accepted_emoji` (`accepted_emoji_id`),
  CONSTRAINT `fk_bounties_accepted_emoji` FOREIGN KEY (`accepted_emoji_id`) REFERENCES `emojis` (`id`),
  CONSTRAINT `fk_bounties_accepted_user` FOREIGN KEY (`accepted_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_bounties_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='悬赏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bounties`
--

LOCK TABLES `bounties` WRITE;
/*!40000 ALTER TABLE `bounties` DISABLE KEYS */;
INSERT INTO `bounties` VALUES (1,5,'求一张能表达\"不想上班\"的表情包','要那种直击灵魂的感觉，被选中就采纳',50,0,NULL,NULL,'2026-07-07 21:43:04'),(2,4,'需要一个怼领导的熊猫人表情','隐晦一点，不能太明显，懂的都懂',30,0,NULL,NULL,'2026-07-07 21:43:04'),(3,1,'要一张能镇住全场群聊的表情','那种一发出来就没人敢接话的',40,0,NULL,NULL,'2026-07-07 21:43:04'),(4,3,'有没有那种\"老板来了\"的表情包','适合在公司摸鱼群用的',25,0,NULL,NULL,'2026-07-07 21:43:04'),(5,5,'求一个\"这班不上也罢\"的辞职表情','自由职业的快乐谁懂',60,2,NULL,NULL,'2026-07-07 21:43:04'),(6,2,'求一个\"我错了下次还敢\"的表情','要有那种欠揍的感觉',20,1,3,4,'2026-07-07 21:43:04');
/*!40000 ALTER TABLE `bounties` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bounty_submissions`
--

DROP TABLE IF EXISTS `bounty_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bounty_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '投稿ID',
  `bounty_id` bigint NOT NULL COMMENT '所属悬赏',
  `submitter_id` bigint NOT NULL COMMENT '提交者',
  `emoji_id` bigint NOT NULL COMMENT '提交的表情包',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bounty_submit_emoji` (`bounty_id`,`submitter_id`,`emoji_id`),
  KEY `idx_bounty` (`bounty_id`),
  KEY `fk_bounty_sub_user` (`submitter_id`),
  KEY `fk_bounty_sub_emoji` (`emoji_id`),
  CONSTRAINT `fk_bounty_sub_bounty` FOREIGN KEY (`bounty_id`) REFERENCES `bounties` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bounty_sub_emoji` FOREIGN KEY (`emoji_id`) REFERENCES `emojis` (`id`),
  CONSTRAINT `fk_bounty_sub_user` FOREIGN KEY (`submitter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='悬赏投稿表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bounty_submissions`
--

LOCK TABLES `bounty_submissions` WRITE;
/*!40000 ALTER TABLE `bounty_submissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `bounty_submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comments`
--

LOCK TABLES `comments` WRITE;
/*!40000 ALTER TABLE `comments` DISABLE KEYS */;
INSERT INTO `comments` VALUES (3,43,1,'manba out','2026-07-08 16:43:14');
/*!40000 ALTER TABLE `comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `community_posts`
--

DROP TABLE IF EXISTS `community_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `caption` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `like_count` int NOT NULL,
  `comment_count` int NOT NULL,
  `is_deleted` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  `updated_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `community_posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `community_posts`
--

LOCK TABLES `community_posts` WRITE;
/*!40000 ALTER TABLE `community_posts` DISABLE KEYS */;
INSERT INTO `community_posts` VALUES (23,3,'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=200&h=250&fit=crop','Monday blues','周一,崩溃',26,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(24,3,'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=200&h=250&fit=crop','Doggo approves','狗,开心',34,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(25,3,'https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=200&h=250&fit=crop','Puppy eyes','狗,可爱',39,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(26,3,'https://images.unsplash.com/photo-1574158622682-e40e69881006?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1574158622682-e40e69881006?w=200&h=250&fit=crop','Surprised cat','猫,震惊',48,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(27,3,'https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=200&h=250&fit=crop','Cool cat','猫,酷',58,0,0,'2026-07-08 16:37:21','2026-07-10 14:46:02'),(28,5,'https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=200&h=250&fit=crop','Funny face','搞笑,表情',72,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(29,5,'https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=200&h=250&fit=crop','Orange cat','猫,橘猫',52,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(30,5,'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=200&h=250&fit=crop','Sleepy cat','猫,困',69,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(31,5,'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=200&h=250&fit=crop','Side eye cat','猫,鄙视',27,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(32,5,'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=200&h=250&fit=crop','Grumpy cat','猫,生气',21,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(33,2,'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1513245543132-31f507417b26?w=200&h=250&fit=crop','Laughing','笑,开心',26,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(34,2,'https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=200&h=250&fit=crop','Confused','迷惑,问号',79,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(35,2,'https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=200&h=250&fit=crop','Angry birb','鸟,生气',52,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(36,2,'https://images.unsplash.com/photo-1552944150-6dd1180e5999?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1552944150-6dd1180e5999?w=200&h=250&fit=crop','Suspicious','怀疑,眯眼',62,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(37,2,'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=200&h=250&fit=crop','Shiba scream','柴犬,尖叫',28,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(38,4,'https://images.unsplash.com/photo-1561948955-570b270e7c36?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1561948955-570b270e7c36?w=200&h=250&fit=crop','Begging cat','猫,乞求',77,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(39,4,'https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=200&h=250&fit=crop','Derp face','搞笑,呆',28,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(40,4,'https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=200&h=250&fit=crop','Judging you','猫,审判',56,0,0,'2026-07-08 16:37:21','2026-07-08 16:37:21'),(41,4,'https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=200&h=250&fit=crop','Smug cat','猫,得意',69,0,0,'2026-07-08 16:37:21','2026-07-10 14:46:01'),(42,4,'https://images.unsplash.com/photo-1608848461950-0fe51dfc41cb?w=400&h=500&fit=crop','https://images.unsplash.com/photo-1608848461950-0fe51dfc41cb?w=200&h=250&fit=crop','Wide eyes','猫,震惊',29,0,0,'2026-07-08 16:37:21','2026-07-10 12:11:36'),(43,1,'https://imgs.qiubiaoqing.com/qiubiaoqing/imgs/69a503cab1c07q6p.jpeg',NULL,NULL,'kobe',1,1,0,'2026-07-08 16:42:38','2026-07-09 16:17:53'),(44,1,'https://ts4.tc.mm.bing.net/th/id/OIP-C.fr8x3YJpYPfStQUOdJ8CNgHaIn?r=0&cb=thfc1falcon4&rs=1&pid=ImgDetMain&o=7&rm=3',NULL,'man',NULL,0,0,0,'2026-07-09 16:10:26','2026-07-09 16:10:26'),(45,1,'https://q7.itc.cn/images01/20241021/4dfd047bc926499fb0915f1b1bcf84f7.png',NULL,NULL,NULL,0,0,0,'2026-07-09 16:17:19','2026-07-09 16:17:19'),(46,1,'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0',NULL,NULL,NULL,0,0,0,'2026-07-09 16:18:42','2026-07-09 16:18:42'),(47,1,'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0',NULL,NULL,NULL,0,0,0,'2026-07-09 16:18:46','2026-07-09 16:18:46'),(48,1,'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0',NULL,NULL,NULL,0,0,0,'2026-07-09 16:18:47','2026-07-09 16:18:47'),(49,1,'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0',NULL,NULL,NULL,0,0,0,'2026-07-09 16:19:20','2026-07-09 16:19:20'),(50,1,'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0',NULL,NULL,NULL,0,0,0,'2026-07-09 16:22:14','2026-07-09 16:22:14'),(51,1,'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0',NULL,NULL,NULL,0,0,0,'2026-07-09 16:22:14','2026-07-09 16:22:14'),(52,1,'https://imgs.qiubiaoqing.com/qiubiaoqing/imgs/6848696def6f9lGl.jpeg',NULL,NULL,NULL,0,0,0,'2026-07-09 16:29:51','2026-07-09 16:29:51'),(53,1,'https://c-ssl.duitang.com/uploads/blog/202312/22/EWSq85ezUVwpzGp.jpg',NULL,NULL,NULL,0,0,0,'2026-07-09 16:30:18','2026-07-09 16:30:18'),(54,1,'https://i-blog.csdnimg.cn/img_convert/91b8381abe29594e7f2c1c50b46991cb.png',NULL,'哈哈哈哈哈哈','搞笑',0,0,1,'2026-07-10 12:09:52','2026-07-10 15:00:55'),(55,1,'http://10.0.2.2:9000/static/memes/397c1250999478f34c39899a1b1c3561.png',NULL,'111111','123',0,0,1,'2026-07-10 14:33:57','2026-07-10 15:01:28'),(56,1,'http://10.0.2.2:9000/static/memes/397c1250999478f34c39899a1b1c3561.png',NULL,'哈哈哈哈哈','搞笑',0,0,1,'2026-07-10 15:00:30','2026-07-10 15:01:33'),(57,1,'http://10.0.2.2:9000/static/memes/8e592b3696059b5863e35dc24f4d7fad.png',NULL,'哈哈哈哈哈','1111111',0,0,1,'2026-07-10 15:09:41','2026-07-10 15:09:49');
/*!40000 ALTER TABLE `community_posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `danmaku_rooms`
--

DROP TABLE IF EXISTS `danmaku_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `danmaku_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  `name` varchar(50) NOT NULL COMMENT '房间名称 → BattleRoom.name',
  `creator_id` bigint NOT NULL COMMENT '创建者',
  `max_users` int NOT NULL DEFAULT '50' COMMENT '人数上限（PRD要求50人）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0=关闭, 1=活跃',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_active_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间（2h无活动自动关闭）',
  PRIMARY KEY (`id`),
  KEY `idx_status_active` (`status`,`last_active_at` DESC),
  KEY `fk_danmaku_creator` (`creator_id`),
  CONSTRAINT `fk_danmaku_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='斗图房间表（弹幕消息不持久化，走Redis/WebSocket）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `danmaku_rooms`
--

LOCK TABLES `danmaku_rooms` WRITE;
/*!40000 ALTER TABLE `danmaku_rooms` DISABLE KEYS */;
/*!40000 ALTER TABLE `danmaku_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `emoji_tags`
--

DROP TABLE IF EXISTS `emoji_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emoji_tags` (
  `emoji_id` bigint NOT NULL COMMENT '表情包ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`emoji_id`,`tag_id`),
  KEY `fk_emoji_tags_tag` (`tag_id`),
  CONSTRAINT `fk_emoji_tags_emoji` FOREIGN KEY (`emoji_id`) REFERENCES `emojis` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_emoji_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表情包-标签关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `emoji_tags`
--

LOCK TABLES `emoji_tags` WRITE;
/*!40000 ALTER TABLE `emoji_tags` DISABLE KEYS */;
INSERT INTO `emoji_tags` VALUES (1,1),(3,1),(5,1),(7,1),(8,1),(2,2),(6,2),(3,3),(6,4),(1,5),(2,6),(3,7),(4,7),(4,8),(7,9),(8,9),(5,10),(8,10);
/*!40000 ALTER TABLE `emoji_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `emojis`
--

DROP TABLE IF EXISTS `emojis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emojis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表情包ID',
  `file_url` varchar(512) NOT NULL COMMENT '原图存储路径（MinIO/本地）',
  `thumbnail_url` varchar(512) DEFAULT NULL COMMENT '缩略图路径（瀑布流加载用）',
  `file_md5` varchar(32) NOT NULL COMMENT 'MD5去重，防重复上传',
  `width` int DEFAULT NULL COMMENT '图片宽度px',
  `height` int DEFAULT NULL COMMENT '图片高度px',
  `format` varchar(8) DEFAULT NULL COMMENT 'jpg/png/gif/webp',
  `file_size` int DEFAULT NULL COMMENT '文件大小Bytes',
  `uploader_id` bigint NOT NULL COMMENT '上传者',
  `description` varchar(200) DEFAULT NULL COMMENT '文字描述 → MemeItem.title，同时用于CLIP文本编码',
  `source_type` tinyint NOT NULL DEFAULT '0' COMMENT '0=用户上传, 1=种子数据, 2=社区创作',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0=待审核, 1=正常, 2=屏蔽',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数冗余 → MemeItem.likeCount',
  `collect_count` int NOT NULL DEFAULT '0' COMMENT '收藏数冗余',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数冗余 → MemeItem.commentCount',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间 → MemeItem.createdAt',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_md5` (`file_md5`),
  KEY `idx_uploader` (`uploader_id`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_like_count` (`like_count` DESC),
  KEY `idx_created_at` (`created_at` DESC),
  KEY `idx_source_like` (`source_type`,`status`,`like_count` DESC),
  CONSTRAINT `fk_emojis_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表情包表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `emojis`
--

LOCK TABLES `emojis` WRITE;
/*!40000 ALTER TABLE `emojis` DISABLE KEYS */;
INSERT INTO `emojis` VALUES (1,'https://images.unsplash.com/photo-1560807707-8cc77767d783?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1560807707-8cc77767d783?w=150&h=150&fit=crop','a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4',800,600,'jpg',102400,1,'周一早上我的精神状态',1,1,3,1,2,'2026-07-07 21:43:04'),(2,'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=150&h=150&fit=crop','b2c3d4e5f6a7b2c3d4e5f6a7b2c3d4e5',720,720,'jpg',89600,2,'打工人打工魂打工都是人上人',1,1,2,0,0,'2026-07-07 21:43:04'),(3,'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=150&h=150&fit=crop','c3d4e5f6a7b8c3d4e5f6a7b8c3d4e5f6',600,800,'gif',204800,1,'猫咪歪头杀',2,1,2,1,1,'2026-07-07 21:43:04'),(4,'https://images.unsplash.com/photo-1574158622682-e40e69881006?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1574158622682-e40e69881006?w=150&h=150&fit=crop','d4e5f6a7b8c9d4e5f6a7b8c9d4e5f6a7',640,640,'webp',51200,3,'真的假的我不信',2,1,4,3,2,'2026-07-07 21:43:04'),(5,'https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=150&h=150&fit=crop','e5f6a7b8c9d0e5f6a7b8c9d0e5f6a7b8',1080,1080,'png',307200,1,'给你一个大大的赞',1,1,1,0,0,'2026-07-07 21:43:04'),(6,'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1513245543132-31f507417b26?w=150&h=150&fit=crop','f6a7b8c9d0e1f6a7b8c9d0e1f6a7b8c9',500,500,'gif',45000,2,'别说了在做了在做了',2,1,1,0,0,'2026-07-07 21:43:04'),(7,'https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=150&h=150&fit=crop','a7b8c9d0e1f2a7b8c9d0e1f2a7b8c9d0',750,750,'jpg',76800,4,'无敌是多么寂寞',1,1,3,1,1,'2026-07-07 21:43:04'),(8,'https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=300&h=300&fit=crop','https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=150&h=150&fit=crop','b8c9d0e1f2a3b8c9d0e1f2a3b8c9d0e1',900,600,'webp',61440,5,'菜就多练输不起就别玩',2,1,5,4,2,'2026-07-07 21:43:04'),(9,'/static/memes/397c1250999478f34c39899a1b1c3561.png','/static/memes/397c1250999478f34c39899a1b1c3561.png','397c1250999478f34c39899a1b1c3561',NULL,NULL,NULL,NULL,1,'community_post',0,1,0,0,0,'2026-07-10 14:33:50'),(10,'/static/memes/35d9b44cb11d1be31340ca6fd5eb2537.png','/static/memes/35d9b44cb11d1be31340ca6fd5eb2537.png','35d9b44cb11d1be31340ca6fd5eb2537',NULL,NULL,NULL,NULL,1,'test-emoji-123',0,1,0,0,0,'2026-07-10 15:05:17'),(11,'/static/memes/8e592b3696059b5863e35dc24f4d7fad.png','/static/memes/8e592b3696059b5863e35dc24f4d7fad.png','8e592b3696059b5863e35dc24f4d7fad',NULL,NULL,NULL,NULL,1,'community_post',0,1,0,0,0,'2026-07-10 15:09:33'),(12,'/static/memes/0759e798d0f0d5eac15cb4156b3c6d78.png','/static/memes/0759e798d0f0d5eac15cb4156b3c6d78.png','0759e798d0f0d5eac15cb4156b3c6d78',NULL,NULL,NULL,NULL,1,'upload',0,1,0,0,0,'2026-07-10 15:10:08'),(13,'/static/memes/ba49d0f2768bb5b6a3a4803afab2d9cb.png','/static/memes/ba49d0f2768bb5b6a3a4803afab2d9cb.png','ba49d0f2768bb5b6a3a4803afab2d9cb',NULL,NULL,NULL,NULL,1,'upload',0,1,0,0,0,'2026-07-10 15:14:58');
/*!40000 ALTER TABLE `emojis` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `user_id` bigint NOT NULL COMMENT '收藏者',
  `emoji_id` bigint NOT NULL COMMENT '被收藏的表情包',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`user_id`,`emoji_id`),
  KEY `fk_favorites_emoji` (`emoji_id`),
  CONSTRAINT `fk_favorites_emoji` FOREIGN KEY (`emoji_id`) REFERENCES `emojis` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
INSERT INTO `favorites` VALUES (1,3,'2026-07-07 21:43:04'),(1,4,'2026-07-07 21:43:04'),(1,8,'2026-07-07 21:43:04'),(2,4,'2026-07-07 21:43:04'),(2,7,'2026-07-07 21:43:04'),(2,8,'2026-07-07 21:43:04'),(3,1,'2026-07-07 21:43:04'),(3,8,'2026-07-07 21:43:04'),(4,4,'2026-07-07 21:43:04'),(4,8,'2026-07-07 21:43:04');
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `follows`
--

DROP TABLE IF EXISTS `follows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `follows` (
  `follower_id` bigint NOT NULL,
  `following_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`follower_id`,`following_id`),
  KEY `following_id` (`following_id`),
  CONSTRAINT `follows_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`),
  CONSTRAINT `follows_ibfk_2` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `follows`
--

LOCK TABLES `follows` WRITE;
/*!40000 ALTER TABLE `follows` DISABLE KEYS */;
INSERT INTO `follows` VALUES (1,2,'2026-07-10 14:45:40'),(1,3,'2026-07-10 12:11:21'),(1,5,'2026-07-09 16:08:07'),(2,1,'2026-07-10 13:40:02'),(2,3,'2026-07-10 13:40:02'),(2,4,'2026-07-10 13:40:02'),(3,1,'2026-07-10 13:40:02'),(3,2,'2026-07-10 13:40:02'),(4,1,'2026-07-10 13:40:02'),(4,2,'2026-07-10 13:40:02'),(4,3,'2026-07-10 13:40:02'),(4,5,'2026-07-10 13:40:02'),(5,1,'2026-07-10 13:40:02');
/*!40000 ALTER TABLE `follows` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_matches`
--

DROP TABLE IF EXISTS `game_matches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_matches` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对局ID',
  `room_id` bigint DEFAULT NULL,
  `round_num` int DEFAULT '0',
  `target_emoji_id` varchar(100) NOT NULL COMMENT 'emoji key',
  `target_label` varchar(100) NOT NULL DEFAULT '' COMMENT '模仿目标标签',
  `target_image` varchar(512) NOT NULL DEFAULT '' COMMENT '目标表情包URL',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=匹配中, 1=进行中, 2=已结束',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_room_round` (`room_id`,`round_num`),
  KEY `fk_game_target_emoji` (`target_emoji_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模仿大赛对局表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `game_matches`
--

LOCK TABLES `game_matches` WRITE;
/*!40000 ALTER TABLE `game_matches` DISABLE KEYS */;
/*!40000 ALTER TABLE `game_matches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_participants`
--

DROP TABLE IF EXISTS `game_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_participants` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `match_id` bigint NOT NULL COMMENT '所属对局',
  `user_id` bigint NOT NULL COMMENT '参与者 (默认=1)',
  `score` int NOT NULL DEFAULT '0' COMMENT 'AI评分 0-100',
  `feature_json` text COMMENT '用户特征向量JSON',
  `photo_url` varchar(512) DEFAULT NULL COMMENT '用户模仿自拍的图片路径',
  `photo_label` varchar(100) DEFAULT NULL COMMENT '识别到的标签',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_user` (`match_id`,`user_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_user_score` (`user_id`,`score` DESC),
  CONSTRAINT `fk_game_part_match` FOREIGN KEY (`match_id`) REFERENCES `game_matches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对局参与者表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `game_participants`
--

LOCK TABLES `game_participants` WRITE;
/*!40000 ALTER TABLE `game_participants` DISABLE KEYS */;
/*!40000 ALTER TABLE `game_participants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `likes`
--

DROP TABLE IF EXISTS `likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_like_post_user` (`post_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `likes_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `likes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `likes`
--

LOCK TABLES `likes` WRITE;
/*!40000 ALTER TABLE `likes` DISABLE KEYS */;
INSERT INTO `likes` VALUES (6,43,1,'2026-07-09 16:17:53'),(7,42,1,'2026-07-10 12:11:36');
/*!40000 ALTER TABLE `likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户',
  `type` varchar(24) NOT NULL COMMENT 'like/comment/follow/bounty_accepted/challenge_result',
  `content` varchar(200) NOT NULL COMMENT '通知文案，如「表情帝 赞了你的表情包」',
  `related_id` bigint DEFAULT NULL COMMENT '关联对象ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0=未读, 1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_unread` (`user_id`,`is_read`,`created_at` DESC),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_collects`
--

DROP TABLE IF EXISTS `post_collects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_collects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_collect_post_user` (`post_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `post_collects_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `post_collects_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_collects`
--

LOCK TABLES `post_collects` WRITE;
/*!40000 ALTER TABLE `post_collects` DISABLE KEYS */;
/*!40000 ALTER TABLE `post_collects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_history`
--

DROP TABLE IF EXISTS `search_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户',
  `query` varchar(200) NOT NULL COMMENT '检索词/图片路径',
  `query_type` varchar(16) NOT NULL COMMENT 'text/image/camera',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检索时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_at` DESC),
  CONSTRAINT `fk_search_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检索历史表（30天自动清理）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_history`
--

LOCK TABLES `search_history` WRITE;
/*!40000 ALTER TABLE `search_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(32) NOT NULL COMMENT '标签名，如「搞笑」「猫」「打工人」',
  `usage_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_usage` (`usage_count` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tags`
--

LOCK TABLES `tags` WRITE;
/*!40000 ALTER TABLE `tags` DISABLE KEYS */;
INSERT INTO `tags` VALUES (1,'搞笑',1240,'2026-07-07 21:43:04'),(2,'打工人',890,'2026-07-07 21:43:04'),(3,'猫咪',756,'2026-07-07 21:43:04'),(4,'摸鱼',632,'2026-07-07 21:43:04'),(5,'周一',520,'2026-07-07 21:43:04'),(6,'社恐',488,'2026-07-07 21:43:04'),(7,'吃瓜',401,'2026-07-07 21:43:04'),(8,'扎心',356,'2026-07-07 21:43:04'),(9,'熊猫人',312,'2026-07-07 21:43:04'),(10,'真香',201,'2026-07-07 21:43:04');
/*!40000 ALTER TABLE `tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `topic_submissions`
--

DROP TABLE IF EXISTS `topic_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `topic_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `vote_count` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  KEY `topic_id` (`topic_id`),
  KEY `user_id` (`user_id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `topic_submissions_ibfk_1` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `topic_submissions_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `topic_submissions_ibfk_3` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topic_submissions`
--

LOCK TABLES `topic_submissions` WRITE;
/*!40000 ALTER TABLE `topic_submissions` DISABLE KEYS */;
INSERT INTO `topic_submissions` VALUES (2,3,1,44,27,'2026-07-09 16:10:27'),(3,2,1,45,1,'2026-07-09 16:17:20'),(6,4,1,52,1,'2026-07-09 16:29:51'),(7,4,1,53,1,'2026-07-09 16:30:18');
/*!40000 ALTER TABLE `topic_submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `topic_votes`
--

DROP TABLE IF EXISTS `topic_votes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic_votes` (
  `submission_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`submission_id`,`user_id`),
  UNIQUE KEY `uq_topic_vote_user` (`submission_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `topic_votes_ibfk_1` FOREIGN KEY (`submission_id`) REFERENCES `topic_submissions` (`id`),
  CONSTRAINT `topic_votes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topic_votes`
--

LOCK TABLES `topic_votes` WRITE;
/*!40000 ALTER TABLE `topic_votes` DISABLE KEYS */;
INSERT INTO `topic_votes` VALUES (6,1,'2026-07-09 16:29:52'),(7,1,'2026-07-09 16:30:23');
/*!40000 ALTER TABLE `topic_votes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `topics`
--

DROP TABLE IF EXISTS `topics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_time` datetime NOT NULL DEFAULT (now()),
  `end_time` datetime NOT NULL,
  `status` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topics`
--

LOCK TABLES `topics` WRITE;
/*!40000 ALTER TABLE `topics` DISABLE KEYS */;
INSERT INTO `topics` VALUES (1,'用表情包表达周一的心情','周一起不来床？周一老板开会？用表情包说出你的周一故事！',NULL,'2026-07-08 15:59:33','2026-07-09 15:59:33',0),(2,'假如猫会说话','猫猫的内心OS是什么样的？用表情包帮你家的主子发声！',NULL,'2026-07-08 15:59:33','2026-07-10 15:59:33',1),(3,'当代大学生的期末状态','期末考试、论文、答辩…你的精神状态还好吗？来一张图证明你没疯！',NULL,'2026-07-08 15:59:33','2026-07-13 15:59:33',1),(4,'甲方说「再改一版」时我的反应','用表情包演绎收到反馈时的内心波澜…',NULL,'2026-07-08 15:59:33','2026-07-11 15:59:33',1),(5,'打工人的午餐图鉴','今天中午吃了啥？晒出你的打工人午餐！（注意：用表情包形式）',NULL,'2026-07-08 15:59:33','2026-07-15 15:59:33',1);
/*!40000 ALTER TABLE `topics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(32) NOT NULL COMMENT '登录用户名',
  `password_hash` varchar(128) NOT NULL COMMENT 'bcrypt加密后的密码',
  `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像URL',
  `nickname` varchar(32) DEFAULT NULL COMMENT '显示昵称',
  `bio` varchar(200) DEFAULT NULL COMMENT '个人简介',
  `points` int NOT NULL DEFAULT '0' COMMENT '积分（悬赏/模仿大赛用）',
  `follower_count` int NOT NULL DEFAULT '0' COMMENT '粉丝数冗余',
  `following_count` int NOT NULL DEFAULT '0' COMMENT '关注数冗余',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'testuser','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'测试用户','这个人很懒，什么都没写~',100,4,3,'2026-07-07 21:43:04','2026-07-10 15:11:22'),(2,'emojiking','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'表情帝','热爱表情包创作',200,3,3,'2026-07-07 21:43:04','2026-07-10 14:45:40'),(3,'catlover','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'猫猫教主','铲屎官一枚~',150,3,2,'2026-07-07 21:43:04','2026-07-10 13:40:02'),(4,'meme_maker','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'表情大师','专业生产表情包',300,1,4,'2026-07-07 21:43:04','2026-07-10 15:11:22'),(5,'doutu_boss','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'斗图狂魔','斗图从没输过',500,2,1,'2026-07-07 21:43:04','2026-07-10 13:40:02'),(6,'shenhui','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'神回复','我就是那个神回复',50,0,0,'2026-07-07 21:43:04','2026-07-07 21:43:04'),(7,'newbie','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W',NULL,'萌新一号','刚来，请多关照~',0,0,0,'2026-07-07 21:43:04','2026-07-07 21:43:04');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-10 15:17:34
