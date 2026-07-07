-- ═══════════════════════════════════════════════════════
--  表情云库 — 17 张表建表语句
--  数据库: MySQL 8.0
--  执行方式: mysql -u root -p --default-character-set=utf8mb4 memecloud < 02_create_tables.sql
-- ═══════════════════════════════════════════════════════

SET NAMES utf8mb4;
USE memecloud;


-- ═══════════════════════════════════════════════════
--  1/17  users  用户表
-- ═══════════════════════════════════════════════════
CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    username        VARCHAR(32)     NOT NULL                 COMMENT '登录用户名',
    password_hash   VARCHAR(128)    NOT NULL                 COMMENT 'bcrypt加密后的密码',
    avatar_url      VARCHAR(512)    DEFAULT NULL             COMMENT '头像URL',
    nickname        VARCHAR(32)     DEFAULT NULL             COMMENT '显示昵称',
    bio             VARCHAR(200)    DEFAULT NULL             COMMENT '个人简介',
    points          INT             NOT NULL DEFAULT 0       COMMENT '积分（悬赏/模仿大赛用）',
    follower_count  INT             NOT NULL DEFAULT 0       COMMENT '粉丝数冗余',
    following_count INT             NOT NULL DEFAULT 0       COMMENT '关注数冗余',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '注册时间',
    updated_at      DATETIME        NOT NULL DEFAULT NOW() ON UPDATE NOW()
                                                             COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- ═══════════════════════════════════════════════════
--  2/17  emojis  表情包表
-- ═══════════════════════════════════════════════════
CREATE TABLE emojis (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '表情包ID',
    file_url        VARCHAR(512)    NOT NULL                 COMMENT '原图存储路径（MinIO/本地）',
    thumbnail_url   VARCHAR(512)    DEFAULT NULL             COMMENT '缩略图路径（瀑布流加载用）',
    file_md5        VARCHAR(32)     NOT NULL                 COMMENT 'MD5去重，防重复上传',
    width           INT             DEFAULT NULL             COMMENT '图片宽度px',
    height          INT             DEFAULT NULL             COMMENT '图片高度px',
    format          VARCHAR(8)      DEFAULT NULL             COMMENT 'jpg/png/gif/webp',
    file_size       INT             DEFAULT NULL             COMMENT '文件大小Bytes',
    uploader_id     BIGINT          NOT NULL                 COMMENT '上传者',
    description     VARCHAR(200)    DEFAULT NULL             COMMENT '文字描述 → MemeItem.title，同时用于CLIP文本编码',
    source_type     TINYINT         NOT NULL DEFAULT 0       COMMENT '0=用户上传, 1=种子数据, 2=社区创作',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '0=待审核, 1=正常, 2=屏蔽',
    like_count      INT             NOT NULL DEFAULT 0       COMMENT '点赞数冗余 → MemeItem.likeCount',
    collect_count   INT             NOT NULL DEFAULT 0       COMMENT '收藏数冗余',
    comment_count   INT             NOT NULL DEFAULT 0       COMMENT '评论数冗余 → MemeItem.commentCount',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '上传时间 → MemeItem.createdAt',

    PRIMARY KEY (id),
    UNIQUE KEY uk_file_md5 (file_md5),
    KEY idx_uploader (uploader_id),
    KEY idx_source_type (source_type),
    KEY idx_like_count (like_count DESC),
    KEY idx_created_at (created_at DESC),
    KEY idx_source_like (source_type, status, like_count DESC),

    CONSTRAINT fk_emojis_uploader
        FOREIGN KEY (uploader_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表情包表';


-- ═══════════════════════════════════════════════════
--  3/17  tags  标签表
-- ═══════════════════════════════════════════════════
CREATE TABLE tags (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '标签ID',
    name            VARCHAR(32)     NOT NULL                 COMMENT '标签名，如「搞笑」「猫」「打工人」',
    usage_count     INT             NOT NULL DEFAULT 0       COMMENT '使用次数',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    KEY idx_usage (usage_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';


-- ═══════════════════════════════════════════════════
--  4/17  emoji_tags  表情包-标签关联表
-- ═══════════════════════════════════════════════════
CREATE TABLE emoji_tags (
    emoji_id        BIGINT          NOT NULL                 COMMENT '表情包ID',
    tag_id          BIGINT          NOT NULL                 COMMENT '标签ID',

    PRIMARY KEY (emoji_id, tag_id),

    CONSTRAINT fk_emoji_tags_emoji
        FOREIGN KEY (emoji_id) REFERENCES emojis(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_emoji_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表情包-标签关联表';


-- ═══════════════════════════════════════════════════
--  5/17  favorites  收藏表
-- ═══════════════════════════════════════════════════
CREATE TABLE favorites (
    user_id         BIGINT          NOT NULL                 COMMENT '收藏者',
    emoji_id        BIGINT          NOT NULL                 COMMENT '被收藏的表情包',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '收藏时间',

    PRIMARY KEY (user_id, emoji_id),

    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_favorites_emoji
        FOREIGN KEY (emoji_id) REFERENCES emojis(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';


-- ═══════════════════════════════════════════════════
--  6/17  likes  点赞表
-- ═══════════════════════════════════════════════════
CREATE TABLE likes (
    user_id         BIGINT          NOT NULL                 COMMENT '点赞者',
    emoji_id        BIGINT          NOT NULL                 COMMENT '被点赞的表情包',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '点赞时间',

    PRIMARY KEY (user_id, emoji_id),

    CONSTRAINT fk_likes_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_likes_emoji
        FOREIGN KEY (emoji_id) REFERENCES emojis(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';


-- ═══════════════════════════════════════════════════
--  7/17  comments  评论表
-- ═══════════════════════════════════════════════════
CREATE TABLE comments (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '评论ID',
    user_id         BIGINT          NOT NULL                 COMMENT '评论者',
    emoji_id        BIGINT          NOT NULL                 COMMENT '被评论的表情包',
    content         VARCHAR(200)    NOT NULL                 COMMENT '评论内容（上限200字）',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '评论时间',

    PRIMARY KEY (id),
    KEY idx_emoji (emoji_id),
    KEY idx_user (user_id),

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_comments_emoji
        FOREIGN KEY (emoji_id) REFERENCES emojis(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';


-- ═══════════════════════════════════════════════════
--  8/17  follows  关注表
-- ═══════════════════════════════════════════════════
CREATE TABLE follows (
    follower_id     BIGINT          NOT NULL                 COMMENT '发起关注的人',
    following_id    BIGINT          NOT NULL                 COMMENT '被关注的人',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '关注时间',

    PRIMARY KEY (follower_id, following_id),
    KEY idx_following (following_id),

    CONSTRAINT fk_follows_follower
        FOREIGN KEY (follower_id) REFERENCES users(id),

    CONSTRAINT fk_follows_following
        FOREIGN KEY (following_id) REFERENCES users(id),

    CONSTRAINT chk_no_self_follow
        CHECK (follower_id <> following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';


-- ═══════════════════════════════════════════════════
--  9/17  search_history  检索历史表
-- ═══════════════════════════════════════════════════
CREATE TABLE search_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户',
    query           VARCHAR(200)    NOT NULL                 COMMENT '检索词/图片路径',
    query_type      VARCHAR(16)     NOT NULL                 COMMENT 'text/image/camera',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '检索时间',

    PRIMARY KEY (id),
    KEY idx_user_time (user_id, created_at DESC),

    CONSTRAINT fk_search_history_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检索历史表（30天自动清理）';


-- ═══════════════════════════════════════════════════
--  10/17  topics  话题挑战表
-- ═══════════════════════════════════════════════════
CREATE TABLE topics (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '话题ID',
    title           VARCHAR(100)    NOT NULL                 COMMENT '如「用表情包表达周一的心情」',
    description     VARCHAR(500)    DEFAULT NULL             COMMENT '话题说明',
    cover_url       VARCHAR(512)    DEFAULT NULL             COMMENT '封面图',
    start_time      DATETIME        NOT NULL                 COMMENT '开始时间',
    end_time        DATETIME        NOT NULL                 COMMENT '截止时间',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '0=草稿, 1=进行中, 2=已结束',

    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题挑战表';


-- ═══════════════════════════════════════════════════
--  11/17  topic_submissions  话题投稿表
-- ═══════════════════════════════════════════════════
CREATE TABLE topic_submissions (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '投稿ID',
    topic_id        BIGINT          NOT NULL                 COMMENT '所属话题',
    user_id         BIGINT          NOT NULL                 COMMENT '投稿者',
    emoji_id        BIGINT          NOT NULL                 COMMENT '投稿的表情包',
    vote_count      INT             NOT NULL DEFAULT 0       COMMENT '得票数',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '投稿时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_user_emoji (topic_id, user_id, emoji_id),
    KEY idx_topic (topic_id),
    KEY idx_user (user_id),

    CONSTRAINT fk_topic_sub_topic
        FOREIGN KEY (topic_id) REFERENCES topics(id),

    CONSTRAINT fk_topic_sub_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_topic_sub_emoji
        FOREIGN KEY (emoji_id) REFERENCES emojis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题投稿表';


-- ═══════════════════════════════════════════════════
--  12/17  bounties  悬赏表
-- ═══════════════════════════════════════════════════
CREATE TABLE bounties (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '悬赏ID',
    publisher_id        BIGINT          NOT NULL                 COMMENT '发布者',
    title               VARCHAR(100)    NOT NULL                 COMMENT '悬赏标题',
    description         VARCHAR(500)    DEFAULT NULL             COMMENT '详细描述',
    points_reward       INT             NOT NULL                 COMMENT '悬赏积分（发布时扣除）',
    status              TINYINT         NOT NULL DEFAULT 0       COMMENT '0=进行中, 1=已采纳, 2=已关闭',
    accepted_user_id    BIGINT          DEFAULT NULL             COMMENT '被采纳者',
    accepted_emoji_id   BIGINT          DEFAULT NULL             COMMENT '被采纳的表情包',
    created_at          DATETIME        NOT NULL DEFAULT NOW()   COMMENT '发布时间',

    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_publisher (publisher_id),

    CONSTRAINT fk_bounties_publisher
        FOREIGN KEY (publisher_id) REFERENCES users(id),

    CONSTRAINT fk_bounties_accepted_user
        FOREIGN KEY (accepted_user_id) REFERENCES users(id),

    CONSTRAINT fk_bounties_accepted_emoji
        FOREIGN KEY (accepted_emoji_id) REFERENCES emojis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏表';


-- ═══════════════════════════════════════════════════
--  13/17  bounty_submissions  悬赏投稿表  🆕
-- ═══════════════════════════════════════════════════
CREATE TABLE bounty_submissions (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '投稿ID',
    bounty_id       BIGINT          NOT NULL                 COMMENT '所属悬赏',
    submitter_id    BIGINT          NOT NULL                 COMMENT '提交者',
    emoji_id        BIGINT          NOT NULL                 COMMENT '提交的表情包',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '提交时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_bounty_submit_emoji (bounty_id, submitter_id, emoji_id),
    KEY idx_bounty (bounty_id),

    CONSTRAINT fk_bounty_sub_bounty
        FOREIGN KEY (bounty_id) REFERENCES bounties(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bounty_sub_user
        FOREIGN KEY (submitter_id) REFERENCES users(id),

    CONSTRAINT fk_bounty_sub_emoji
        FOREIGN KEY (emoji_id) REFERENCES emojis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏投稿表';


-- ═══════════════════════════════════════════════════
--  14/17  danmaku_rooms  斗图房间表
-- ═══════════════════════════════════════════════════
CREATE TABLE danmaku_rooms (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '房间ID',
    name            VARCHAR(50)     NOT NULL                 COMMENT '房间名称 → BattleRoom.name',
    creator_id      BIGINT          NOT NULL                 COMMENT '创建者',
    max_users       INT             NOT NULL DEFAULT 50      COMMENT '人数上限（PRD要求50人）',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '0=关闭, 1=活跃',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '创建时间',
    last_active_at  DATETIME        NOT NULL DEFAULT NOW()   COMMENT '最后活跃时间（2h无活动自动关闭）',

    PRIMARY KEY (id),
    KEY idx_status_active (status, last_active_at DESC),

    CONSTRAINT fk_danmaku_creator
        FOREIGN KEY (creator_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='斗图房间表（弹幕消息不持久化，走Redis/WebSocket）';


-- ═══════════════════════════════════════════════════
--  15/17  game_matches  模仿大赛对局表
-- ═══════════════════════════════════════════════════
CREATE TABLE game_matches (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '对局ID',
    room_id             BIGINT          DEFAULT NULL             COMMENT '所属斗图房间（可为NULL，独立匹配）',
    round_num           INT             NOT NULL                 COMMENT '第几轮',
    target_emoji_id     BIGINT          NOT NULL                 COMMENT '本轮模仿目标 →「熊猫人震惊」',
    status              TINYINT         NOT NULL DEFAULT 0       COMMENT '0=匹配中, 1=进行中, 2=已结束',

    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_room_round (room_id, round_num),

    CONSTRAINT fk_game_room
        FOREIGN KEY (room_id) REFERENCES danmaku_rooms(id),

    CONSTRAINT fk_game_target_emoji
        FOREIGN KEY (target_emoji_id) REFERENCES emojis(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模仿大赛对局表';


-- ═══════════════════════════════════════════════════
--  16/17  game_participants  对局参与者表  🆕
-- ═══════════════════════════════════════════════════
CREATE TABLE game_participants (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    match_id        BIGINT          NOT NULL                 COMMENT '所属对局',
    user_id         BIGINT          NOT NULL                 COMMENT '参与者',
    score           INT             NOT NULL DEFAULT 0       COMMENT 'AI评分 0-100',
    photo_url       VARCHAR(512)    DEFAULT NULL             COMMENT '用户模仿自拍的图片路径',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '参与时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_match_user (match_id, user_id),
    KEY idx_user (user_id),
    KEY idx_user_score (user_id, score DESC),

    CONSTRAINT fk_game_part_match
        FOREIGN KEY (match_id) REFERENCES game_matches(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_game_part_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对局参与者表';


-- ═══════════════════════════════════════════════════
--  17/17  notifications  通知表  🆕
-- ═══════════════════════════════════════════════════
CREATE TABLE notifications (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '通知ID',
    user_id         BIGINT          NOT NULL                 COMMENT '接收通知的用户',
    type            VARCHAR(24)     NOT NULL                 COMMENT 'like/comment/follow/bounty_accepted/challenge_result',
    content         VARCHAR(200)    NOT NULL                 COMMENT '通知文案，如「表情帝 赞了你的表情包」',
    related_id      BIGINT          DEFAULT NULL             COMMENT '关联对象ID',
    is_read         TINYINT         NOT NULL DEFAULT 0       COMMENT '0=未读, 1=已读',
    created_at      DATETIME        NOT NULL DEFAULT NOW()   COMMENT '通知时间',

    PRIMARY KEY (id),
    KEY idx_user_unread (user_id, is_read, created_at DESC),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';


-- ═══════════════════════════════════════════════════════
--  验证：显示所有表
-- ═══════════════════════════════════════════════════════
SHOW TABLES;
