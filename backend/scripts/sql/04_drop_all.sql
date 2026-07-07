-- ═══════════════════════════════════════════════════════
--  表情云库 — 清空/删除脚本（仅测试环境使用！）
--  执行方式: mysql -u root -p --default-character-set=utf8mb4 < 04_drop_all.sql
-- ═══════════════════════════════════════════════════════

SET NAMES utf8mb4;

-- ⚠️ 警告：此脚本会删除所有表和数据，不可恢复！

-- 安全模式：先禁用外键检查，再逐表删除
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS game_participants;
DROP TABLE IF EXISTS game_matches;
DROP TABLE IF EXISTS danmaku_rooms;
DROP TABLE IF EXISTS bounty_submissions;
DROP TABLE IF EXISTS bounties;
DROP TABLE IF EXISTS topic_submissions;
DROP TABLE IF EXISTS topics;
DROP TABLE IF EXISTS search_history;
DROP TABLE IF EXISTS follows;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS likes;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS emoji_tags;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS emojis;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- 同时也删库
DROP DATABASE IF EXISTS memecloud;

SELECT 'All tables and database dropped.';
