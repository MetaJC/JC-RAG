-- =============================================================================
-- 023_user_memory_profile.sql
-- -----------------------------------------------------------------------------
-- 目的: L3 跨会话画像 (用户级粗粒度结构化画像), 由 L2 长期事实聚合并按 LLM 提炼
-- 业务规则:
--   1. 每用户唯一一条画像 (user_id UNIQUE)
--   2. profile_json 五段结构: domain / role / skills[] / preferences{} / goals[]
--   3. 异步触发: 每累计 N 条 L2 fact 后, 调 LLM 提炼成结构化画像写本表
--   4. 失败兜底: 旧画像不覆盖, 仅记日志
-- 顺序: 在 022 之后执行
-- 兼容: 不可重复执行 (字段/表创建前均做存在性检测)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 用户记忆画像表 (L3 跨会话画像)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_memory_profile (
    id                  BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '主键',
    user_id             BIGINT       NOT NULL                              COMMENT '用户 ID (唯一)',
    profile_json        JSON         NOT NULL                              COMMENT '结构化画像 (domain/role/skills/preferences/goals)',
    version             INT          NOT NULL DEFAULT 1                    COMMENT '画像版本 (每次更新 +1)',
    fact_count          INT          NOT NULL DEFAULT 0                    COMMENT '提取时引用的 L2 fact 数',
    model_name          VARCHAR(64)  DEFAULT NULL                          COMMENT '生成画像使用的模型名',
    last_extracted_at   DATETIME     DEFAULT NULL                          COMMENT '最近一次提取时间',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profile (user_id),
    KEY idx_extracted_at (last_extracted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户跨会话画像 (L3)';
