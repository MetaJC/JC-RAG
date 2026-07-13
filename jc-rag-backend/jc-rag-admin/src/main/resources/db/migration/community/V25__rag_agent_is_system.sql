-- =============================================================================
-- 025_rag_agent_is_system.sql
-- -----------------------------------------------------------------------------
-- 目的: 区分 sys_agents 表中"系统智能体"和"用户智能体"
-- 业务背景:
--   sys_agents 原本只用于 Admin 管控的内置智能体
--   新增 is_system 字段:
--     1 = 系统智能体 (Admin 创建, 全局共享, C 端可见)
--     0 = 用户智能体 (从 user_custom_agent 同步到 sys_agents 用于 Admin 端统一查看)
-- 兼容: 已使用 ALTER TABLE 增量更新, 此文件仅做记录 (再次执行安全)
-- =============================================================================

-- 增量加列 (重复执行安全: MySQL 8 不支持 IF NOT EXISTS 在 ADD COLUMN,
-- 实际生产环境用 Flyway/Liquibase 管理; 此处保留 ALTER 供手工回放)
ALTER TABLE sys_agents
    ADD COLUMN IF NOT EXISTS is_system TINYINT NOT NULL DEFAULT 1
        COMMENT '是否系统智能体: 1=系统(Admin管控) 0=用户'
        AFTER agent_key;

-- 索引 (提升 is_system 过滤性能)
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_agents'
      AND INDEX_NAME = 'idx_is_system'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_is_system ON sys_agents(is_system)',
    'SELECT "idx_is_system already exists" AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 数据回填: 历史数据全部默认 is_system=1 (默认 1 已生效, 此处仅做兜底确认)
UPDATE sys_agents SET is_system = 1 WHERE is_system IS NULL OR is_system NOT IN (0, 1);
