<h1 align="center">MemeCloud · 表情云库</h1>

<p align="center">
  <em>基于 AI 映射的表情包众包管理与社交共享平台</em>
</p>

<p align="center">
  <strong>Android</strong> — <strong>FastAPI</strong> — <strong>MediaPipe</strong> — <strong>Vue.js</strong> — <strong>WebSocket</strong>
</p>

---

## Overview · 项目概述

MemeCloud 是一个面向年轻用户的**表情包管理与社交分享平台**，核心特色在于利用 **AI 视觉映射** 让用户通过摄像头手势、面部表情甚至实时自拍来快速检索和触发表情包。

| 模块 | 技术栈 | 说明 |
|------|--------|------|
| Android 客户端 | Kotlin, Jetpack Compose, CameraX, MediaPipe | 原生相机预览 + AI 手势/表情识别 + KNN 匹配 |
| Backend API | FastAPI, SQLAlchemy, MySQL, WebSocket, JWT | 认证、表情包管理、社区、弹幕对战、游戏 |
| Danmaku Room | Vue.js, WebSocket | 实时弹幕表情对战房间 |

---

## Architecture · 系统架构

```
meme_cloud/
├── android/                   # Android 原生客户端
│   └── app/src/main/java/com/memecloud/
│       ├── ui/                # UI 层（首页 / 搜索 / 社区 / 对战 / 个人中心）
│       ├── data/              # 数据层（API 接口 / 数据模型 / 网络配置）
│       ├── features/          # 端侧 AI（GestureClassifier）
│       └── di/                # 依赖注入
│
├── backend/                   # FastAPI 后端服务
│   └── app/
│       ├── main.py            # 应用入口 + 启动初始化
│       ├── core/              # 数据库 / 安全 / 配置
│       ├── models/            # ORM 模型（用户/表情包/社区/弹幕/游戏）
│       ├── schemas/           # Pydantic 请求/响应模型
│       ├── services/          # 业务逻辑（KNN 匹配 / MediaPipe / 弹幕 / 游戏）
│       └── api/routes/        # RESTful 路由（auth / match / danmaku / game / community / social）
│
│
├── docs/                      # 项目文档
│   ├── 表情云库_PRD_完整版.md
│   ├── 项目选题报告.md
│   ├── 项目代码组织架构.md
│   └── DATABASE.md
│
└── memecloud_dump.sql         # 数据库结构快照
```

---

## Key Features · 核心功能

### AI 智能检索

- **拍照映射检索** — 通过摄像头拍照，AI 分析画面中的姿态、手势与表情，从库中匹配最相似的表情包
- **手势识别匹配** — 基于 MediaPipe 提取 22 维手部特征向量，使用 KNN 算法实时分类
- **面部表情识别** — 提取面部 Blendshape 特征向量，支持自定义表情训练与识别
- **文字语义搜索** — 支持基于名称/标签的关键词搜索
- **以图搜图** — 上传图片查找相似表情包

### Android 客户端

- **相机实时预览** — CameraX + 覆盖层实时显示姿态骨架与手部关键点
- **多模式匹配** — 手势模式 / 表情模式 一键切换
- **个人表情库** — 收藏、管理自己的表情包集合
- **表情包社区** — 浏览、发布、点赞、评论、排行榜、悬赏求图

### 实时弹幕对战

- **WebSocket 实时通信** — 加入/创建房间，实时收发表情包弹幕
- **弹幕滚动播放** — 表情包从右侧飞入，自动循环播放
- **表情包发送** — 从个人库或搜索中选择表情包，一键发送
- **随机进入** — 一键随机加入有位置的房间
- **旁观模式** — 满员时仍可观看

### 社区与社交

- **社区广场** — 浏览推荐表情包，热门排行
- **发布表情包** — 用户上传自己的表情包到社区
- **话题挑战** — 参与特定话题的表情包创作
- **模仿大赛** — AI 评估表情包模仿相似度
- **悬赏求图** — 发布悬赏寻求特定表情包
- **关注体系** — 关注用户，查看社交动态

---

## Tech Stack · 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| **FastAPI** | 异步 Web 框架 |
| **SQLAlchemy** | ORM 数据库操作 |
| **MySQL (PyMySQL)** | 数据库存储 |
| **python-jose + bcrypt** | JWT 认证 & 密码哈希 |
| **OpenCV** | 图像处理 |
| **MediaPipe** | 手部/姿态/面部关键点提取 |
| **WebSocket** | 实时弹幕通信 |
| **Pydantic** | 数据校验与序列化 |

### Android 客户端

| 技术 | 用途 |
|------|------|
| **Kotlin** | 主开发语言 |
| **Jetpack Compose** | UI 框架 |
| **CameraX** | 相机预览与拍照 |
| **Retrofit** | HTTP 网络请求 |
| **MediaPipe** | 端侧姿态/手部/面部 AI 模型 |
| **WebSocket** | 实时对战通信 |

### 前端 Web

| 技术 | 用途 |
|------|------|
| **Vue.js** | 响应式 UI 框架 |
| **MediaPipe Tasks-Vision** | 浏览器端姿态/手势/面部 AI 模型 |

---

## Getting Started · 快速开始

### Prerequisites

- Python 3.10+
- Android Studio Hedgehog+
- MySQL 8.0+

### Backend

```bash
cd backend
pip install -r requirements.txt

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 9000 --reload
```

```

### Android 客户端

用 Android Studio 打开 `android/` 目录，同步 Gradle 后运行。应用默认连接 `10.0.2.2:9000`（模拟器），可修改 `ServerConfig.kt` 中的服务器地址。

---

## API Overview · 接口概览

| 路由 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/match/gesture` | POST | 手势匹配 |
| `/api/match/expression` | POST | 表情匹配 |
| `/api/match/record` | POST | 记录样本 |
| `/api/community/memes` | GET/POST | 社区表情包列表/发布 |
| `/api/danmaku/rooms` | GET/POST | 弹幕房间列表/创建 |
| `/api/danmaku/ws/{room_id}` | WebSocket | 弹幕房间实时连接 |
| `/api/danmaku/rooms/random` | POST | 随机进入房间 |
| `/api/social/follow` | POST | 关注/取消关注 |
| `/api/game/match` | POST | 游戏匹配 |
| `/api/game/ws` | WebSocket | 游戏实时对战 |

---

## Design Highlights · 设计亮点

### AI 映射检索闭环

```
用户拍照/自拍
    ↓
MediaPipe 提取姿态 + 手势 + 面部特征
    ↓
KNN 分类器匹配最相似的表情包标签
    ↓
从库中召回对应表情包展示
    ↓
用户反馈 → 增量记录样本 → 匹配精度持续提升
```

### 众包增长模式

表情包库在消费与生产的双轮驱动下持续增长：

- **消费侧**：拍照/文字/以图搜图 → 消费表情包 → 点赞/收藏
- **生产侧**：上传表情包 → AI 打标入库 → 社区发布 → 互动激励


---

## Project Background · 项目背景

在日常社交聊天中，用户经常遇到"此时想发一个表情包但翻遍收藏夹也找不到"的困境。传统表情包管理依赖关键词命名、人工分类和最近使用记录，这与人的记忆模式不完全一致——用户往往记住的是画面感（一个人皱眉的画面、一只夸张的猫脸、一种崩溃的姿势），而非具体标签。

MemeCloud 的核心切入点：**用 AI 视觉能力打通"视觉记忆 → 表情包检索"的通路**，让用户用最自然的拍照、手势、表情等方式找到想要的表情包。

---

## License

MIT

<p align="center">
  <sub>Built with ❤️ by the MemeCloud Team</sub>
</p>
