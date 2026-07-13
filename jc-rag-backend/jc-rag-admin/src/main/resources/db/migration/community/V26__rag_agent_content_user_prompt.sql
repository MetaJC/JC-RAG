-- =============================================================================
-- 026_rag_agent_content_user_prompt.sql
-- -----------------------------------------------------------------------------
-- 目的: sys_agents 区分 system prompt 与 user prompt, 与 user_custom_agent 对齐
-- 业务背景:
--   sys_agents 现有 system_prompt 字段仅用于"系统智能体" (is_system=1)
--   "内建用户智能体" (is_system=0) 的提示词语义上是 user-layer, 走 user prompt
--   新增 content 字段: 内建用户智能体使用此字段作为 user prompt 内容
-- 兼容: 增量加列, 已使用 IF NOT EXISTS 保护
-- =============================================================================

ALTER TABLE sys_agents
    ADD COLUMN IF NOT EXISTS content TEXT NULL
        COMMENT '内建用户智能体 user prompt (is_system=0 时使用); 系统智能体不用此字段'
        AFTER system_prompt;
