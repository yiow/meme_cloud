# 表情云库 (MemeCloud) 数据库文档

> 数据库: MySQL 8.0, utf8mb4  
> 导出日期: 2026-07-09  
> SQL 导入文件: `memecloud_dump.sql`

---

## 一、测试账号（共 7 个）

所有账号密码均为 `123456`。

| 用户名 | 昵称 | 发出帖子数 | 简介 |
|---|---|---|---|
| `testuser` | 测试用户 | 11 | 这个人很懒，什么都没写~ |
| `emojiking` | 表情帝 | 5 | 热爱收集表情包 |
| `catlover` | 猫猫控 | 5 | 云吸猫一枚~ |
| `meme_maker` | 表情师 | 5 | 专业制作表情包 |
| `doutu_boss` | 斗图大魔王 | 5 | 斗图还没输过 |
| `shenhui` | 社会我辉哥 | 0 | 我就是那个社会辉 |
| `newbie` | 萌新一只 | 0 | 刚来报到请多关照~ |

---

## 二、社区帖子（community_posts，共 31 条）

帖子采用 Unsplash CDN 真实图片，均以猫/狗/搞怪为主题。按发布者分组：

### catlover（5 条）
| ID | 标题 | 标签 | 点赞 |
|---|---|---|---|
| 23 | Monday blues | 周一, 崩溃 | 26 |
| 24 | Doggo approves | 狗, 开心 | 34 |
| 25 | Puppy eyes | 狗, 可爱 | 39 |
| 26 | Surprised cat | 猫, 震惊 | 48 |
| 27 | Cool cat | 猫, 酷 | 58 |

### doutu_boss（5 条）
| ID | 标题 | 标签 | 点赞 |
|---|---|---|---|
| 28 | Funny face | 搞笑, 表情 | 72 |
| 29 | Orange cat | 猫, 橘猫 | 52 |
| 30 | Sleepy cat | 猫, 困 | 69 |
| 31 | Side eye cat | 猫, 鄙视 | 27 |
| 32 | Grumpy cat | 猫, 生气 | 21 |

### emojiking（5 条）
| ID | 标题 | 标签 | 点赞 |
|---|---|---|---|
| 33 | Laughing | 笑, 开心 | 26 |
| 34 | Confused | 迷惑, 问号 | 79 |
| 35 | Angry birb | 鸟, 生气 | 52 |
| 36 | Suspicious | 怀疑, 眯眼 | 62 |
| 37 | Shiba scream | 柴犬, 尖叫 | 28 |

### meme_maker（5 条）
| ID | 标题 | 标签 | 点赞 |
|---|---|---|---|
| 38 | Begging cat | 猫, 乞求 | 77 |
| 39 | Derp face | 搞笑, 呆 | 28 |
| 40 | Judging you | 猫, 审判 | 56 |
| 41 | Smug cat | 猫, 得意 | 69 |
| 42 | Wide eyes | 猫, 震惊 | 28 |

### testuser（11 条）
除 ID 43 是科比表情包（有 1 条评论和 1 个点赞）外，其余 10 条为用户测试上传。

---

## 三、话题挑战（topics，共 5 个）

| ID | 标题 | 描述 | 状态 | 投稿数 |
|---|---|---|---|---|
| 1 | 用表情包表达周一的心情 | 周一起不来床？周一老板开会？用表情包说出你的周一故事！ | 已结束 | 0 |
| 2 | 假如猫会说话 | 猫猫的内心OS是什么样的？用表情包帮你家的主子发声！ | 进行中 | 1 |
| 3 | 当代大学生的期末状态 | 期末考试、论文、答辩…你的精神状态还好吗？来一张图证明你没疯！ | 进行中 | 1 |
| 4 | 甲方说「再改一版」时我的反应 | 用表情包演绎收到反馈时的内心波澜… | 进行中 | 2 |
| 5 | 打工人的午餐图鉴 | 今天中午吃了啥？晒出你的打工人午餐！ | 进行中 | 0 |

---

## 四、关注关系（follows，共 3 条）

testuser 关注了 3 个人：

- testuser → catlover（猫猫控）
- testuser → meme_maker（表情师）
- testuser → doutu_boss（斗图大魔王）

关注关系双向维护 `follower_count` 和 `following_count`，取关时同步扣减。

---

## 五、种子数据

### emojis 表（8 条）
表情包初始检索库，所有 file_url 指向 `minio.example.com`（占位 URL，不可实际加载）。检索功能依赖此表。每条记录了宽高、格式（jpg/gif/webp/png）、文件大小、MD5 等元数据。

### tags 表（10 条）+ emoji_tags（18 条关联）
预设标签及使用次数：搞笑(1240), 表情(890), 猫图(756), 日常(632), 周一(520), 吐槽(488), 白怪(401), 社恐(356), 熊猫人(312), 打工人(201)。

### favorites 表（10 条）
各用户的收藏记录，收藏对象为 emojis 表的表情包。

### bounties 表（6 条）
待实现的悬赏功能种子数据，含标题、积分悬赏、状态（open/resolved）。

---

## 六、核心表结构速查

### 社区相关
- **community_posts** — 帖子主表，含图片 URL、配文、标签、点赞数、评论数、软删除标记
- **comments** — 一级评论（post_id → community_posts）
- **likes** — 点赞记录（UNIQUE: post_id + user_id）

### 社交相关
- **follows** — 关注关系（联合主键: follower_id + following_id）
- **topics** — 话题挑战（含起止时间、状态）
- **topic_submissions** — 话题投稿（关联 community_posts，同一用户可多次投稿）
- **topic_votes** — 投稿投票（UNIQUE: submission_id + user_id，每人每投稿限一票）

### 用户相关
- **users** — 含 username、密码哈希、头像、昵称、简介、积分、粉丝数/关注数

### 模仿大赛相关
- **game_matches** — 对局主表，含目标表情包（target_emoji_id, target_label, target_image）、状态、创建时间
  - `target_emoji_id`: VARCHAR(100)，表情包标识（非 FK）
  - 无 `fk_game_room` / `fk_game_target_emoji` 外键约束（已删除，因 emoji key 为字符串）
- **game_participants** — 对局参与记录，含 score、feature_json（特征向量）、photo_label（识别标签）
  - 无 `fk_game_part_user` 外键约束（已删除，因 App 使用随机 user_id，DB 存盘默认 user_id=1）
  - UNIQUE(match_id, user_id)

### 斗图相关
- **danmaku_rooms** — 斗图室房间

### 其他（种子数据，功能待完善）
- **emojis** — 表情包检索库
- **tags / emoji_tags** — 标签体系
- **favorites** — 收藏
- **search_history** — 检索历史
- **bounties / bounty_submissions** — 悬赏
- **notifications** — 通知
