-- ============================================================
-- JC RAG 增量脚本：管理端合并与 C 端默认知识库
-- 类型：增量修改
-- 适用对象：
-- 1. 已执行 001/002/003 初始化脚本的现有库
-- 2. 需要把 C 端知识库统一到管理员指定默认知识库的环境
-- 使用说明：
-- 1. 先确认要作为 C 端默认知识库的 `kb_knowledge_base.id`
-- 2. 把下面示例中的 `1` 改成真实知识库 ID
-- 3. 执行本脚本
-- ============================================================

USE jc_rag;

-- 可选：提升“按用户查询会话”和“按会话顺序查询消息”的性能。
-- 这里用 information_schema 做幂等判断，重复执行不会报错。
SET @idx_count = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_session'
      AND index_name = 'idx_chat_session_user_status_updated'
);
SET @sql_stmt = IF(
    @idx_count = 0,
    'CREATE INDEX idx_chat_session_user_status_updated ON chat_session (user_id, status, updated_at)',
    'SELECT ''idx_chat_session_user_status_updated already exists'''
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_count = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_session'
      AND column_name = 'is_pinned'
);
SET @sql_stmt = IF(
    @column_count = 0,
    'ALTER TABLE chat_session ADD COLUMN is_pinned TINYINT NOT NULL DEFAULT 0 AFTER kb_id',
    'SELECT ''chat_session.is_pinned already exists'''
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_count = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_message'
      AND index_name = 'idx_chat_message_session_index'
);
SET @sql_stmt = IF(
    @idx_count = 0,
    'CREATE INDEX idx_chat_message_session_index ON chat_message (session_id, message_index)',
    'SELECT ''idx_chat_message_session_index already exists'''
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_count = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_session'
      AND index_name = 'idx_chat_session_user_status_pin_updated'
);
SET @sql_stmt = IF(
    @idx_count = 0,
    'CREATE INDEX idx_chat_session_user_status_pin_updated ON chat_session (user_id, status, is_pinned, updated_at)',
    'SELECT ''idx_chat_session_user_status_pin_updated already exists'''
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 设置 C 端默认知识库。
-- 注意：请把 config_value 的 1 改成管理员实际要开放给 C 端的知识库 ID。
INSERT INTO sys_config (
    config_group,
    config_key,
    config_value,
    value_type,
    encrypted,
    remark
) VALUES (
    'chat',
    'chat.default.kbId',
    '1',
    'NUMBER',
    0,
    'C 端默认知识库'
)
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    value_type = VALUES(value_type),
    encrypted = VALUES(encrypted),
    remark = VALUES(remark),
    updated_at = CURRENT_TIMESTAMP;
