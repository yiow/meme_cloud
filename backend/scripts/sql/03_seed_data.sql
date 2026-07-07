-- ═══════════════════════════════════════════════════════
--  表情云库 — 种子数据 / 测试数据
--  执行方式: mysql -u root -p --default-character-set=utf8mb4 memecloud < 03_seed_data.sql
--  ⚠️ 此脚本仅供开发/测试环境使用
-- ═══════════════════════════════════════════════════════

SET NAMES utf8mb4;
USE memecloud;


-- ── 测试用户（密码均为 "123456" 的 bcrypt hash）─────
INSERT IGNORE INTO users (username, password_hash, nickname, bio, points) VALUES
('testuser',  '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '测试用户', '这个人很懒，什么都没写~', 100),
('emojiking', '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '表情帝',   '热爱表情包创作',         200),
('catlover',  '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '猫猫教主', '铲屎官一枚~',         150),
('meme_maker','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '表情大师', '专业生产表情包',        300),
('doutu_boss','$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '斗图狂魔', '斗图从没输过',           500),
('shenhui',   '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '神回复',   '我就是那个神回复',        50),
('newbie',    '$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W', '萌新一号', '刚来，请多关照~',         0);


-- ── 测试标签（对应 HomeScreen LazyRow 热门标签）─────
INSERT IGNORE INTO tags (name, usage_count) VALUES
('搞笑',     1240),
('打工人',   890),
('猫咪',     756),
('摸鱼',     632),
('周一',     520),
('社恐',     488),
('吃瓜',     401),
('扎心',     356),
('熊猫人',   312),
('真香',     201);


-- ── 种子表情包 ────────────────────────────────────────
INSERT IGNORE INTO emojis (file_url, thumbnail_url, file_md5, width, height, format, file_size,
                    uploader_id, description, source_type, status, like_count, collect_count, comment_count) VALUES
('https://minio.example.com/emojis/meme_001.jpg',
 'https://minio.example.com/emojis/thumbs/meme_001_thumb.jpg',
 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', 800, 600, 'jpg', 102400,
 1, '周一早上我的精神状态', 1, 1, 328, 45, 12),

('https://minio.example.com/emojis/meme_002.jpg',
 'https://minio.example.com/emojis/thumbs/meme_002_thumb.jpg',
 'b2c3d4e5f6a7b2c3d4e5f6a7b2c3d4e5', 720, 720, 'jpg', 89600,
 2, '打工人打工魂打工都是人上人', 1, 1, 256, 33, 8),

('https://minio.example.com/emojis/meme_003.jpg',
 'https://minio.example.com/emojis/thumbs/meme_003_thumb.jpg',
 'c3d4e5f6a7b8c3d4e5f6a7b8c3d4e5f6', 600, 800, 'gif', 204800,
 1, '猫咪歪头杀', 2, 1, 189, 72, 15),

('https://minio.example.com/emojis/meme_004.jpg',
 'https://minio.example.com/emojis/thumbs/meme_004_thumb.jpg',
 'd4e5f6a7b8c9d4e5f6a7b8c9d4e5f6a7', 640, 640, 'webp', 51200,
 3, '真的假的我不信', 2, 1, 412, 98, 23),

('https://minio.example.com/emojis/meme_005.jpg',
 'https://minio.example.com/emojis/thumbs/meme_005_thumb.jpg',
 'e5f6a7b8c9d0e5f6a7b8c9d0e5f6a7b8', 1080, 1080, 'png', 307200,
 1, '给你一个大大的赞', 1, 1, 156, 21, 5),

('https://minio.example.com/emojis/meme_006.jpg',
 'https://minio.example.com/emojis/thumbs/meme_006_thumb.jpg',
 'f6a7b8c9d0e1f6a7b8c9d0e1f6a7b8c9', 500, 500, 'gif', 45000,
 2, '别说了在做了在做了', 2, 1, 97, 11, 3),

('https://minio.example.com/emojis/meme_007.jpg',
 'https://minio.example.com/emojis/thumbs/meme_007_thumb.jpg',
 'a7b8c9d0e1f2a7b8c9d0e1f2a7b8c9d0', 750, 750, 'jpg', 76800,
 4, '无敌是多么寂寞', 1, 1, 289, 55, 9),

('https://minio.example.com/emojis/meme_008.jpg',
 'https://minio.example.com/emojis/thumbs/meme_008_thumb.jpg',
 'b8c9d0e1f2a3b8c9d0e1f2a3b8c9d0e1', 900, 600, 'webp', 61440,
 5, '菜就多练输不起就别玩', 2, 1, 543, 120, 31);


-- ── 表情包-标签关联 ────────────────────────────────────
INSERT IGNORE INTO emoji_tags (emoji_id, tag_id) VALUES
(1, 1), (1, 5),        -- meme_001: 搞笑, 周一
(2, 2), (2, 6),        -- meme_002: 打工人, 社恐
(3, 3), (3, 1), (3, 7),-- meme_003: 猫咪, 搞笑, 吃瓜
(4, 7), (4, 8),        -- meme_004: 吃瓜, 扎心
(5, 1), (5, 10),       -- meme_005: 搞笑, 真香
(6, 2), (6, 4),        -- meme_006: 打工人, 摸鱼
(7, 9), (7, 1),        -- meme_007: 熊猫人, 搞笑
(8, 9), (8, 10), (8, 1); -- meme_008: 熊猫人, 真香, 搞笑


-- ── 测试关注关系 ────────────────────────────────────────
INSERT IGNORE INTO follows (follower_id, following_id) VALUES
(1, 2), (1, 3), (1, 5),    -- testuser 关注 3人
(2, 1), (2, 3), (2, 4),    -- emojiking 关注 3人
(3, 1), (3, 2),             -- catlover 关注 2人
(4, 1), (4, 2), (4, 3), (4, 5), -- meme_maker 关注 4人
(5, 1);                     -- doutu_boss 关注 1人


-- ── 测试点赞/收藏/评论 ──────────────────────────────────
INSERT IGNORE INTO likes (user_id, emoji_id) VALUES
(1, 4), (1, 7), (1, 8),
(2, 1), (2, 3), (2, 4), (2, 8),
(3, 1), (3, 2), (3, 4), (3, 8),
(4, 2), (4, 5), (4, 7), (4, 8),
(5, 1), (5, 3), (5, 4), (5, 6), (5, 7), (5, 8);

INSERT IGNORE INTO favorites (user_id, emoji_id) VALUES
(1, 3), (1, 4), (1, 8),
(2, 4), (2, 7), (2, 8),
(3, 1), (3, 8),
(4, 4), (4, 8);

INSERT IGNORE INTO comments (user_id, emoji_id, content) VALUES
(2, 1, '太真实了哈哈哈'),
(5, 1, '这就是我本人'),
(1, 3, '这小猫咪也太可爱了叭'),
(5, 4, '我不信.jpg'),
(2, 4, '这表情笑死我了🤣'),
(3, 7, '熊猫人永不认输！'),
(4, 8, '斗图界需要你这样的人才'),
(1, 8, '收藏了就代表我会了');


-- ── 测试话题挑战 ────────────────────────────────────────
INSERT IGNORE INTO topics (title, description, status, start_time, end_time) VALUES
('用表情包表达周一的心情', '周一综合症发作现场，用表情包证明你还活着', 1,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY)),
('我的精神状态 be like', '用一张表情包形容你现在的精神状态', 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY)),
('打工人嘴替', '那些说了打工人才敢说的话的表情包', 2,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('猫猫表情包大赏', '晒出你最爱的猫猫表情包！', 2,
 DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
('扎心瞬间', '被现实暴击的那些瞬间', 1,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY)),
('真香预警', '那些打脸来得太快的名场面', 2,
 DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY));


-- ── 测试悬赏 ────────────────────────────────────────────
-- 未采纳的悬赏
INSERT IGNORE INTO bounties (publisher_id, title, description, points_reward, status) VALUES
(5, '求一张能表达"不想上班"的表情包', '要那种直击灵魂的感觉，被选中就采纳', 50, 0),
(4, '需要一个怼领导的熊猫人表情', '隐晦一点，不能太明显，懂的都懂', 30, 0),
(1, '要一张能镇住全场群聊的表情', '那种一发出来就没人敢接话的', 40, 0),
(3, '有没有那种"老板来了"的表情包', '适合在公司摸鱼群用的', 25, 0),
(5, '求一个"这班不上也罢"的辞职表情', '自由职业的快乐谁懂', 60, 2);

-- 已采纳的悬赏
INSERT IGNORE INTO bounties (publisher_id, title, description, points_reward, status, accepted_user_id, accepted_emoji_id) VALUES
(2, '求一个"我错了下次还敢"的表情', '要有那种欠揍的感觉', 20, 1, 3, 4);


-- ── 更新冗余计数（与插入数据保持一致）──────────────────
UPDATE users u SET
    follower_count  = (SELECT COUNT(*) FROM follows WHERE following_id = u.id),
    following_count = (SELECT COUNT(*) FROM follows WHERE follower_id = u.id);

UPDATE emojis e SET
    like_count    = (SELECT COUNT(*) FROM likes WHERE emoji_id = e.id),
    collect_count = (SELECT COUNT(*) FROM favorites WHERE emoji_id = e.id),
    comment_count = (SELECT COUNT(*) FROM comments WHERE emoji_id = e.id);


-- ── 验证 ─────────────────────────────────────────────────
SELECT 'users' AS tbl, COUNT(*) AS cnt FROM users
UNION ALL SELECT 'emojis', COUNT(*) FROM emojis
UNION ALL SELECT 'tags', COUNT(*) FROM tags
UNION ALL SELECT 'emoji_tags', COUNT(*) FROM emoji_tags
UNION ALL SELECT 'likes', COUNT(*) FROM likes
UNION ALL SELECT 'favorites', COUNT(*) FROM favorites
UNION ALL SELECT 'comments', COUNT(*) FROM comments
UNION ALL SELECT 'follows', COUNT(*) FROM follows
UNION ALL SELECT 'topics', COUNT(*) FROM topics
UNION ALL SELECT 'bounties', COUNT(*) FROM bounties;
