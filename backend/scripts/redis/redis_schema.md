# 表情云库 — Redis 数据结构与脚本

> 版本：v1.0 | Redis 7.x | 与 MySQL 互补，负责缓存/实时/排行榜场景

---

## 目录

- [一、连接信息](#一连接信息)
- [二、Key 命名规范](#二key-命名规范)
- [三、弹幕斗图房](#三弹幕斗图房)
- [四、排行榜](#四排行榜)
- [五、模仿大赛匹配](#五模仿大赛匹配)
- [六、Session / JWT 黑名单](#六session--jwt-黑名单)
- [七、验证码](#七验证码)
- [八、表情包热度缓存](#八表情包热度缓存)
- [九、限流](#九限流)
- [十、初始化脚本](#十初始化脚本)
- [十一、常用运维命令](#十一常用运维命令)

---

## 一、连接信息

```
redis-server / redis-cli
默认: 127.0.0.1:6379
db: 0  — 业务缓存
db: 1  — Session / JWT黑名单
db: 2  — 限流 & 验证码
```

---

## 二、Key 命名规范

```
项目前缀: 服务名:实体:ID  (用冒号分层)

mc:danmaku:room:5:messages     — 斗图房 5 的消息列表
mc:rank:today                  — 今日排行榜 ZSET
mc:rank:weekly                 — 本周排行榜 ZSET
mc:match:queue                 — 模仿大赛匹配队列 LIST
mc:match:session:123:round:2   — 对局 123-第2轮 倒计时 STRING
mc:session:user:42             — 用户 42 的 JWT session STRING
mc:blacklist:jwt:tokenabc      — 已吊销的 JWT
mc:captcha:user:42             — 用户 42 的验证码 STRING
mc:hot:emojis                  — 热门表情包缓存 ZSET
mc:rate:login:192.168.1.1      — 登录限流 STRING
```

---

## 三、弹幕斗图房

> 选题报告明确：弹幕消息不持久化。进房取最近 20 条，WebSocket 实时推送。

### Key 设计

```
mc:danmaku:room:{room_id}:messages   LIST (LEFT PUSH, LTRIM保留最近20条)
mc:danmaku:room:{room_id}:online     SET  (在线用户ID集合)
```

### 命令

```redis
# 用户进入房间
SADD mc:danmaku:room:5:online 42

# 发送弹幕（插入头部，保留最近20条）
LPUSH mc:danmaku:room:5:messages '{"user_id":42,"nickname":"表情帝","content":"🐼熊猫人出击","time":1713001234}'
LTRIM mc:danmaku:room:5:messages 0 19

# 拉取最近弹幕（新用户进房）
LRANGE mc:danmaku:room:5:messages 0 19

# 用户离开房间
SREM mc:danmaku:room:5:online 42

# 房间关闭（清理 Redis）
DEL mc:danmaku:room:5:messages
DEL mc:danmaku:room:5:online
```

### 后端伪代码（FastAPI + WebSocket）

```python
# 进房
@app.websocket("/ws/room/{room_id}")
async def danmaku_ws(ws: WebSocket, room_id: int, user_id: int):
    await ws.accept()
    await redis.sadd(f"mc:danmaku:room:{room_id}:online", user_id)

    # 推送最近 20 条历史
    recent = await redis.lrange(f"mc:danmaku:room:{room_id}:messages", 0, 19)
    for msg in reversed(recent):
        await ws.send_text(msg.decode())

    # 订阅房间频道，广播新消息
    pubsub = redis.pubsub()
    await pubsub.subscribe(f"mc:danmaku:room:{room_id}:channel")
    async for event in pubsub.listen():
        await ws.send_text(event["data"].decode())

# 发送弹幕
async def send_danmaku(room_id: int, user_id: int, nickname: str, content: str):
    msg = json.dumps({"user_id": user_id, "nickname": nickname, "content": content, "time": int(time.time())})
    await redis.lpush(f"mc:danmaku:room:{room_id}:messages", msg)
    await redis.ltrim(f"mc:danmaku:room:{room_id}:messages", 0, 19)
    await redis.publish(f"mc:danmaku:room:{room_id}:channel", msg)
```

---

## 四、排行榜

> RankingScreen — 今日最火 / 本周最热 / 总榜，按 like_count 排序。

### 策略

- ZSET，key 带时间粒度，定时刷新（非实时计算，减少 DB 压力）
- 每分钟从 MySQL `emojis` 表同步 top N 进入 Redis

### Key 设计

```
mc:rank:today          ZSET  member=emoji_id  score=今日新增点赞数
mc:rank:weekly         ZSET  member=emoji_id  score=本周新增点赞数
mc:rank:total          ZSET  member=emoji_id  score=总like_count
```

### 更新（定时任务，每分钟执行）

```redis
# 清除旧数据
DEL mc:rank:today mc:rank:weekly mc:rank:total

# 今日排行（likes 表当天创建的 count）
ZADD mc:rank:today 328 1 256 2 189 3 412 4 156 5 97 6 289 7 543 8

# 本周排行
ZADD mc:rank:weekly 420 1 310 2 230 3 512 4 180 5 120 6 350 7 610 8

# 总榜
ZADD mc:rank:total 328 1 256 2 189 3 412 4 156 5 97 6 289 7 543 8
```

### 查询（客户端调用）

```redis
# Top 20 今日最火（降序）
ZREVRANGE mc:rank:today 0 19 WITHSCORES

# 某个表情包的排名
ZREVRANK mc:rank:today 3

# 某个表情包的分数
ZSCORE mc:rank:today 3
```

---

## 五、模仿大赛匹配

> ImitationContestScreen — 匹配阶段等待 2-5 秒，凑够人后开始。

### Key 设计

```
mc:match:queue                  LIST   待匹配用户队列 (LPUSH / BRPOP 阻塞取)
mc:match:session:{match_id}     HASH   对局信息 (room_id, round_num, target_emoji_id, status)
mc:match:countdown:{match_id}   STRING 倒计时秒数 (TTL 控制)
mc:match:players:{match_id}     SET    本局参与者
```

### 匹配流程

```redis
# 1. 用户点击"开始匹配" → 入队
LPUSH mc:match:queue 42

# 2. 后端匹配服务 — 阻塞等待凑够 4 人（最多等 5s）
BRPOP mc:match:queue 5   # 返回("mc:match:queue", "42")

# 3. 凑够人后创建对局
HSET mc:match:session:123 room_id 5 round_num 1 target_emoji_id 8 status playing

# 4. 设置 15s 倒计时
SETEX mc:match:countdown:123 15 "15"

# 5. 记录参与者
SADD mc:match:players:123 42 1 3 5

# 6. 对局结束后 AI 打分 → 写入 MySQL game_participants → 清理 Redis
DEL mc:match:session:123 mc:match:countdown:123 mc:match:players:123
```

---

## 六、Session / JWT 黑名单

### Key 设计

```
mc:session:user:{user_id}      STRING  JWT token (TTL = 7天)
mc:blacklist:jwt:{token_hash}  STRING  "revoked" (TTL = 剩余有效期)
```

### 命令

```redis
# 登录 — 存 session
SETEX mc:session:user:42 604800 "eyJhbGciOiJIUzI1NiIs..."

# 校验 token
GET mc:session:user:42

# 登出 — 加入黑名单
SETEX mc:blacklist:jwt:eyJhbGciOi... 604800 "revoked"
DEL mc:session:user:42

# 中间件验证 — 先查黑名单
EXISTS mc:blacklist:jwt:eyJhbGciOi...
```

---

## 七、验证码

```redis
# 发送验证码（60s过期）
SETEX mc:captcha:login:13800138000 60 "482917"

# 校验
GET mc:captcha:login:13800138000

# 每个IP/手机号每天最多5次
INCR mc:captcha:limit:13800138000
EXPIRE mc:captcha:limit:13800138000 86400
```

---

## 八、表情包热度缓存

> 社区广场瀑布流、首页推荐等高频查询走 Redis。

```redis
# 热门表情包（按点赞数，每5分钟刷新）
ZADD mc:hot:emojis 543 8 412 4 328 1 289 7 256 2

# 取前50
ZREVRANGE mc:hot:emojis 0 49 WITHSCORES

# 某表情包热度+1（点赞时同时更新）
ZINCRBY mc:hot:emojis 1 3
```

---

## 九、限流

```redis
# 登录 — 每IP每分钟最多5次
INCR mc:rate:login:192.168.1.1
EXPIRE mc:rate:login:192.168.1.1 60

# 发弹幕 — 每用户每秒最多2条
INCR mc:rate:danmaku:user:42
EXPIRE mc:rate:danmaku:user:42 1

# API全局限流 — 每用户每分钟最多60次
INCR mc:rate:api:user:42
EXPIRE mc:rate:api:user:42 60
```

---

## 十、初始化脚本

```redis
# 切换到 db 0（业务缓存）
SELECT 0

# 预建排行榜（空 ZSET，定时任务填充）
ZADD mc:rank:today 0 __placeholder__
ZADD mc:rank:weekly 0 __placeholder__
ZADD mc:rank:total 0 __placeholder__

# 切换到 db 1（Session）
SELECT 1

# 切换到 db 2（限流）
SELECT 2

# 确认
SELECT 0
KEYS mc:*
```

---

## 十一、常用运维命令

```redis
# 查看所有 key
KEYS mc:*

# 查看内存占用
INFO memory

# 查看连接数
INFO clients | grep connected_clients

# 清理过期 session（手动触发）
SCAN 0 MATCH mc:session:* COUNT 100

# 清空所有 mc 前缀的 key（仅测试环境！）
EVAL "return redis.call('del', unpack(redis.call('keys', ARGV[1])))" 0 mc:*

# 监控实时命令
MONITOR

# 查看慢查询
SLOWLOG GET 10
```
