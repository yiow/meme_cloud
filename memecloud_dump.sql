-- ============================================================
-- MemeCloud Database Dump
-- Generated: 2026-07-09
-- MySQL 8.0, utf8mb4
-- Usage: mysql -u root -p < memecloud_dump.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS memecloud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE memecloud;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `bounties`;
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

INSERT INTO `bounties` (`id`, `publisher_id`, `title`, `description`, `points_reward`, `status`, `accepted_user_id`, `accepted_emoji_id`, `created_at`) VALUES (1, 5, '求一张能表达"不想上班"的表情包', '要那种直击灵魂的感觉，被选中就采纳', 50, 0, NULL, NULL, '2026-07-07 21:43:04');
INSERT INTO `bounties` (`id`, `publisher_id`, `title`, `description`, `points_reward`, `status`, `accepted_user_id`, `accepted_emoji_id`, `created_at`) VALUES (2, 4, '需要一个怼领导的熊猫人表情', '隐晦一点，不能太明显，懂的都懂', 30, 0, NULL, NULL, '2026-07-07 21:43:04');
INSERT INTO `bounties` (`id`, `publisher_id`, `title`, `description`, `points_reward`, `status`, `accepted_user_id`, `accepted_emoji_id`, `created_at`) VALUES (3, 1, '要一张能镇住全场群聊的表情', '那种一发出来就没人敢接话的', 40, 0, NULL, NULL, '2026-07-07 21:43:04');
INSERT INTO `bounties` (`id`, `publisher_id`, `title`, `description`, `points_reward`, `status`, `accepted_user_id`, `accepted_emoji_id`, `created_at`) VALUES (4, 3, '有没有那种"老板来了"的表情包', '适合在公司摸鱼群用的', 25, 0, NULL, NULL, '2026-07-07 21:43:04');
INSERT INTO `bounties` (`id`, `publisher_id`, `title`, `description`, `points_reward`, `status`, `accepted_user_id`, `accepted_emoji_id`, `created_at`) VALUES (5, 5, '求一个"这班不上也罢"的辞职表情', '自由职业的快乐谁懂', 60, 2, NULL, NULL, '2026-07-07 21:43:04');
INSERT INTO `bounties` (`id`, `publisher_id`, `title`, `description`, `points_reward`, `status`, `accepted_user_id`, `accepted_emoji_id`, `created_at`) VALUES (6, 2, '求一个"我错了下次还敢"的表情', '要有那种欠揍的感觉', 20, 1, 3, 4, '2026-07-07 21:43:04');

DROP TABLE IF EXISTS `bounty_submissions`;
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

-- bounty_submissions: empty table, no data

DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`),
  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `comments` (`id`, `post_id`, `user_id`, `content`, `created_at`) VALUES (3, 43, 1, 'manba out', '2026-07-08 16:43:14');

DROP TABLE IF EXISTS `community_posts`;
CREATE TABLE `community_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `caption` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `like_count` int NOT NULL,
  `comment_count` int NOT NULL,
  `is_deleted` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  `updated_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `community_posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (23, 3, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=200&h=250&fit=crop', 'Monday blues', '周一,崩溃', 26, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (24, 3, 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=200&h=250&fit=crop', 'Doggo approves', '狗,开心', 34, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (25, 3, 'https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=200&h=250&fit=crop', 'Puppy eyes', '狗,可爱', 39, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (26, 3, 'https://images.unsplash.com/photo-1574158622682-e40e69881006?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1574158622682-e40e69881006?w=200&h=250&fit=crop', 'Surprised cat', '猫,震惊', 48, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (27, 3, 'https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=200&h=250&fit=crop', 'Cool cat', '猫,酷', 58, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (28, 5, 'https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=200&h=250&fit=crop', 'Funny face', '搞笑,表情', 72, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (29, 5, 'https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=200&h=250&fit=crop', 'Orange cat', '猫,橘猫', 52, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (30, 5, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=200&h=250&fit=crop', 'Sleepy cat', '猫,困', 69, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (31, 5, 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=200&h=250&fit=crop', 'Side eye cat', '猫,鄙视', 27, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (32, 5, 'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=200&h=250&fit=crop', 'Grumpy cat', '猫,生气', 21, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (33, 2, 'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=200&h=250&fit=crop', 'Laughing', '笑,开心', 26, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (34, 2, 'https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=200&h=250&fit=crop', 'Confused', '迷惑,问号', 79, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (35, 2, 'https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=200&h=250&fit=crop', 'Angry birb', '鸟,生气', 52, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (36, 2, 'https://images.unsplash.com/photo-1552944150-6dd1180e5999?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1552944150-6dd1180e5999?w=200&h=250&fit=crop', 'Suspicious', '怀疑,眯眼', 62, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (37, 2, 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=200&h=250&fit=crop', 'Shiba scream', '柴犬,尖叫', 28, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (38, 4, 'https://images.unsplash.com/photo-1561948955-570b270e7c36?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1561948955-570b270e7c36?w=200&h=250&fit=crop', 'Begging cat', '猫,乞求', 77, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (39, 4, 'https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=200&h=250&fit=crop', 'Derp face', '搞笑,呆', 28, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (40, 4, 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=200&h=250&fit=crop', 'Judging you', '猫,审判', 56, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (41, 4, 'https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=200&h=250&fit=crop', 'Smug cat', '猫,得意', 69, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (42, 4, 'https://images.unsplash.com/photo-1608848461950-0fe51dfc41cb?w=400&h=500&fit=crop', 'https://images.unsplash.com/photo-1608848461950-0fe51dfc41cb?w=200&h=250&fit=crop', 'Wide eyes', '猫,震惊', 28, 0, 0, '2026-07-08 16:37:21', '2026-07-08 16:37:21');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (43, 1, 'https://imgs.qiubiaoqing.com/qiubiaoqing/imgs/69a503cab1c07q6p.jpeg', NULL, NULL, 'kobe', 1, 1, 0, '2026-07-08 16:42:38', '2026-07-09 16:17:53');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (44, 1, 'https://ts4.tc.mm.bing.net/th/id/OIP-C.fr8x3YJpYPfStQUOdJ8CNgHaIn?r=0&cb=thfc1falcon4&rs=1&pid=ImgDetMain&o=7&rm=3', NULL, 'man', NULL, 0, 0, 0, '2026-07-09 16:10:26', '2026-07-09 16:10:26');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (45, 1, 'https://q7.itc.cn/images01/20241021/4dfd047bc926499fb0915f1b1bcf84f7.png', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:17:19', '2026-07-09 16:17:19');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (46, 1, 'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:18:42', '2026-07-09 16:18:42');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (47, 1, 'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:18:46', '2026-07-09 16:18:46');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (48, 1, 'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:18:47', '2026-07-09 16:18:47');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (49, 1, 'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:19:20', '2026-07-09 16:19:20');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (50, 1, 'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:22:14', '2026-07-09 16:22:14');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (51, 1, 'https://ts1.tc.mm.bing.net/th/id/R-C.36646081a283aa9d6392a7f8cbd38f8a?rik=DalkSwt1%2fOJwUw&riu=http%3a%2f%2fk.sinaimg.cn%2fn%2fsinakd20115%2f107%2fw1024h683%2f20241018%2f0b36-6c26de3fc65233b958e6ab6ea9062084.jpg%2fw700d1q75cms.jpg%3fby%3dcms_fixed_width&ehk=UJH31Ckarz0l9ccN6oVRc8keBdRYhfVZAjiWZOByfMY%3d&risl=&pid=ImgRaw&r=0', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:22:14', '2026-07-09 16:22:14');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (52, 1, 'https://imgs.qiubiaoqing.com/qiubiaoqing/imgs/6848696def6f9lGl.jpeg', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:29:51', '2026-07-09 16:29:51');
INSERT INTO `community_posts` (`id`, `user_id`, `image_url`, `thumbnail_url`, `caption`, `tags`, `like_count`, `comment_count`, `is_deleted`, `created_at`, `updated_at`) VALUES (53, 1, 'https://c-ssl.duitang.com/uploads/blog/202312/22/EWSq85ezUVwpzGp.jpg', NULL, NULL, NULL, 0, 0, 0, '2026-07-09 16:30:18', '2026-07-09 16:30:18');

DROP TABLE IF EXISTS `danmaku_rooms`;
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

-- danmaku_rooms: empty table, no data

DROP TABLE IF EXISTS `emoji_tags`;
CREATE TABLE `emoji_tags` (
  `emoji_id` bigint NOT NULL COMMENT '表情包ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`emoji_id`,`tag_id`),
  KEY `fk_emoji_tags_tag` (`tag_id`),
  CONSTRAINT `fk_emoji_tags_emoji` FOREIGN KEY (`emoji_id`) REFERENCES `emojis` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_emoji_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表情包-标签关联表';

INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (1, 1);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (3, 1);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (5, 1);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (7, 1);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (8, 1);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (2, 2);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (6, 2);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (3, 3);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (6, 4);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (1, 5);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (2, 6);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (3, 7);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (4, 7);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (4, 8);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (7, 9);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (8, 9);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (5, 10);
INSERT INTO `emoji_tags` (`emoji_id`, `tag_id`) VALUES (8, 10);

DROP TABLE IF EXISTS `emojis`;
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表情包表';

INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (1, 'https://minio.example.com/emojis/meme_001.jpg', 'https://minio.example.com/emojis/thumbs/meme_001_thumb.jpg', 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', 800, 600, 'jpg', 102400, 1, '周一早上我的精神状态', 1, 1, 3, 1, 2, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (2, 'https://minio.example.com/emojis/meme_002.jpg', 'https://minio.example.com/emojis/thumbs/meme_002_thumb.jpg', 'b2c3d4e5f6a7b2c3d4e5f6a7b2c3d4e5', 720, 720, 'jpg', 89600, 2, '打工人打工魂打工都是人上人', 1, 1, 2, 0, 0, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (3, 'https://minio.example.com/emojis/meme_003.jpg', 'https://minio.example.com/emojis/thumbs/meme_003_thumb.jpg', 'c3d4e5f6a7b8c3d4e5f6a7b8c3d4e5f6', 600, 800, 'gif', 204800, 1, '猫咪歪头杀', 2, 1, 2, 1, 1, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (4, 'https://minio.example.com/emojis/meme_004.jpg', 'https://minio.example.com/emojis/thumbs/meme_004_thumb.jpg', 'd4e5f6a7b8c9d4e5f6a7b8c9d4e5f6a7', 640, 640, 'webp', 51200, 3, '真的假的我不信', 2, 1, 4, 3, 2, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (5, 'https://minio.example.com/emojis/meme_005.jpg', 'https://minio.example.com/emojis/thumbs/meme_005_thumb.jpg', 'e5f6a7b8c9d0e5f6a7b8c9d0e5f6a7b8', 1080, 1080, 'png', 307200, 1, '给你一个大大的赞', 1, 1, 1, 0, 0, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (6, 'https://minio.example.com/emojis/meme_006.jpg', 'https://minio.example.com/emojis/thumbs/meme_006_thumb.jpg', 'f6a7b8c9d0e1f6a7b8c9d0e1f6a7b8c9', 500, 500, 'gif', 45000, 2, '别说了在做了在做了', 2, 1, 1, 0, 0, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (7, 'https://minio.example.com/emojis/meme_007.jpg', 'https://minio.example.com/emojis/thumbs/meme_007_thumb.jpg', 'a7b8c9d0e1f2a7b8c9d0e1f2a7b8c9d0', 750, 750, 'jpg', 76800, 4, '无敌是多么寂寞', 1, 1, 3, 1, 1, '2026-07-07 21:43:04');
INSERT INTO `emojis` (`id`, `file_url`, `thumbnail_url`, `file_md5`, `width`, `height`, `format`, `file_size`, `uploader_id`, `description`, `source_type`, `status`, `like_count`, `collect_count`, `comment_count`, `created_at`) VALUES (8, 'https://minio.example.com/emojis/meme_008.jpg', 'https://minio.example.com/emojis/thumbs/meme_008_thumb.jpg', 'b8c9d0e1f2a3b8c9d0e1f2a3b8c9d0e1', 900, 600, 'webp', 61440, 5, '菜就多练输不起就别玩', 2, 1, 5, 4, 2, '2026-07-07 21:43:04');

DROP TABLE IF EXISTS `favorites`;
CREATE TABLE `favorites` (
  `user_id` bigint NOT NULL COMMENT '收藏者',
  `emoji_id` bigint NOT NULL COMMENT '被收藏的表情包',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`user_id`,`emoji_id`),
  KEY `fk_favorites_emoji` (`emoji_id`),
  CONSTRAINT `fk_favorites_emoji` FOREIGN KEY (`emoji_id`) REFERENCES `emojis` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';

INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (1, 3, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (1, 4, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (1, 8, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (2, 4, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (2, 7, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (2, 8, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (3, 1, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (3, 8, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (4, 4, '2026-07-07 21:43:04');
INSERT INTO `favorites` (`user_id`, `emoji_id`, `created_at`) VALUES (4, 8, '2026-07-07 21:43:04');

DROP TABLE IF EXISTS `follows`;
CREATE TABLE `follows` (
  `follower_id` bigint NOT NULL,
  `following_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`follower_id`,`following_id`),
  KEY `following_id` (`following_id`),
  CONSTRAINT `follows_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`),
  CONSTRAINT `follows_ibfk_2` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `follows` (`follower_id`, `following_id`, `created_at`) VALUES (1, 3, '2026-07-09 16:18:08');
INSERT INTO `follows` (`follower_id`, `following_id`, `created_at`) VALUES (1, 4, '2026-07-08 16:52:17');
INSERT INTO `follows` (`follower_id`, `following_id`, `created_at`) VALUES (1, 5, '2026-07-09 16:08:07');

DROP TABLE IF EXISTS `game_matches`;
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

-- game_matches: empty table, no data

DROP TABLE IF EXISTS `game_participants`;
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

-- game_participants: empty table, no data

DROP TABLE IF EXISTS `likes`;
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `likes` (`id`, `post_id`, `user_id`, `created_at`) VALUES (6, 43, 1, '2026-07-09 16:17:53');

DROP TABLE IF EXISTS `notifications`;
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

-- notifications: empty table, no data

DROP TABLE IF EXISTS `search_history`;
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

-- search_history: empty table, no data

DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(32) NOT NULL COMMENT '标签名，如「搞笑」「猫」「打工人」',
  `usage_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_usage` (`usage_count` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';

INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (1, '搞笑', 1240, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (2, '打工人', 890, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (3, '猫咪', 756, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (4, '摸鱼', 632, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (5, '周一', 520, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (6, '社恐', 488, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (7, '吃瓜', 401, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (8, '扎心', 356, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (9, '熊猫人', 312, '2026-07-07 21:43:04');
INSERT INTO `tags` (`id`, `name`, `usage_count`, `created_at`) VALUES (10, '真香', 201, '2026-07-07 21:43:04');

DROP TABLE IF EXISTS `topic_submissions`;
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

INSERT INTO `topic_submissions` (`id`, `topic_id`, `user_id`, `post_id`, `vote_count`, `created_at`) VALUES (2, 3, 1, 44, 27, '2026-07-09 16:10:27');
INSERT INTO `topic_submissions` (`id`, `topic_id`, `user_id`, `post_id`, `vote_count`, `created_at`) VALUES (3, 2, 1, 45, 1, '2026-07-09 16:17:20');
INSERT INTO `topic_submissions` (`id`, `topic_id`, `user_id`, `post_id`, `vote_count`, `created_at`) VALUES (6, 4, 1, 52, 1, '2026-07-09 16:29:51');
INSERT INTO `topic_submissions` (`id`, `topic_id`, `user_id`, `post_id`, `vote_count`, `created_at`) VALUES (7, 4, 1, 53, 1, '2026-07-09 16:30:18');

DROP TABLE IF EXISTS `topic_votes`;
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

INSERT INTO `topic_votes` (`submission_id`, `user_id`, `created_at`) VALUES (6, 1, '2026-07-09 16:29:52');
INSERT INTO `topic_votes` (`submission_id`, `user_id`, `created_at`) VALUES (7, 1, '2026-07-09 16:30:23');

DROP TABLE IF EXISTS `topics`;
CREATE TABLE `topics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cover_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_time` datetime NOT NULL DEFAULT (now()),
  `end_time` datetime NOT NULL,
  `status` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `topics` (`id`, `title`, `description`, `cover_url`, `start_time`, `end_time`, `status`) VALUES (1, '用表情包表达周一的心情', '周一起不来床？周一老板开会？用表情包说出你的周一故事！', NULL, '2026-07-08 15:59:33', '2026-07-09 15:59:33', 0);
INSERT INTO `topics` (`id`, `title`, `description`, `cover_url`, `start_time`, `end_time`, `status`) VALUES (2, '假如猫会说话', '猫猫的内心OS是什么样的？用表情包帮你家的主子发声！', NULL, '2026-07-08 15:59:33', '2026-07-10 15:59:33', 1);
INSERT INTO `topics` (`id`, `title`, `description`, `cover_url`, `start_time`, `end_time`, `status`) VALUES (3, '当代大学生的期末状态', '期末考试、论文、答辩…你的精神状态还好吗？来一张图证明你没疯！', NULL, '2026-07-08 15:59:33', '2026-07-13 15:59:33', 1);
INSERT INTO `topics` (`id`, `title`, `description`, `cover_url`, `start_time`, `end_time`, `status`) VALUES (4, '甲方说「再改一版」时我的反应', '用表情包演绎收到反馈时的内心波澜…', NULL, '2026-07-08 15:59:33', '2026-07-11 15:59:33', 1);
INSERT INTO `topics` (`id`, `title`, `description`, `cover_url`, `start_time`, `end_time`, `status`) VALUES (5, '打工人的午餐图鉴', '今天中午吃了啥？晒出你的打工人午餐！（注意：用表情包形式）', NULL, '2026-07-08 15:59:33', '2026-07-15 15:59:33', 1);

DROP TABLE IF EXISTS `users`;
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

INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (1, 'testuser', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '测试用户', '这个人很懒，什么都没写~', 100, 4, 6, '2026-07-07 21:43:04', '2026-07-09 16:18:08');
INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (2, 'emojiking', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '表情帝', '热爱表情包创作', 200, 3, 3, '2026-07-07 21:43:04', '2026-07-08 16:02:47');
INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (3, 'catlover', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '猫猫教主', '铲屎官一枚~', 150, 4, 2, '2026-07-07 21:43:04', '2026-07-09 16:18:08');
INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (4, 'meme_maker', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '表情大师', '专业生产表情包', 300, 2, 4, '2026-07-07 21:43:04', '2026-07-08 16:52:17');
INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (5, 'doutu_boss', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '斗图狂魔', '斗图从没输过', 500, 3, 1, '2026-07-07 21:43:04', '2026-07-09 16:08:07');
INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (6, 'shenhui', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '神回复', '我就是那个神回复', 50, 0, 0, '2026-07-07 21:43:04', '2026-07-07 21:43:04');
INSERT INTO `users` (`id`, `username`, `password_hash`, `avatar_url`, `nickname`, `bio`, `points`, `follower_count`, `following_count`, `created_at`, `updated_at`) VALUES (7, 'newbie', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', NULL, '萌新一号', '刚来，请多关照~', 0, 0, 0, '2026-07-07 21:43:04', '2026-07-07 21:43:04');

SET FOREIGN_KEY_CHECKS = 1;
