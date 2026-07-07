-- ═══════════════════════════════════════════════════════
--  表情云库 — 初始化数据库
--  执行方式: mysql -u root -p --default-character-set=utf8mb4 < 01_init_database.sql
-- ═══════════════════════════════════════════════════════

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS memecloud
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 显示已有数据库确认
SHOW DATABASES LIKE 'memecloud';
