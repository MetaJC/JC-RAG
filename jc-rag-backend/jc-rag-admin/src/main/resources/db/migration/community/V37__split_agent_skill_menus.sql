-- ============================================================
-- V37 拆分 Agent 与 Skill 菜单
-- <p>原 menu:agent_manage (Agent 与 Skill) 单页管理过于集中, 拆为:
--   - menu:rag_skill   (技能管理)   /rag/skill
--   - menu:rag_agent   (智能体管理) /rag/agent
-- <p>前端 AgentSkillManageView.vue 已拆分为 RagSkillManageView.vue + RagAgentManageView.vue
-- <p>旧 /agents/manage 路径已重定向到 /rag/agent, menu:agent_manage 仍保留数据但禁用
-- <p>sort=7 与原 agent_manage 保持连续, 视觉位置不变
--
-- @author JC
-- @since 2026-07-25
-- ============================================================

-- 1. 禁用旧菜单 (保留数据, 可回滚)
UPDATE rbac_permission
SET status = 0, visible = 0
WHERE perm_code = 'menu:agent_manage';

-- 2. 新增两个菜单
INSERT INTO rbac_permission
  (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
VALUES
  ('menu:rag_skill', '技能管理',   'MENU', 0, '/rag/skill', 'MagicStick', 7, 'views/RagSkillManageView.vue', 1, 1),
  ('menu:rag_agent', '智能体管理', 'MENU', 0, '/rag/agent', 'Avatar',     7, 'views/RagAgentManageView.vue', 1, 1)
ON DUPLICATE KEY UPDATE
  perm_name = VALUES(perm_name),
  path = VALUES(path),
  icon = VALUES(icon),
  sort = VALUES(sort),
  component = VALUES(component),
  status = 1,
  visible = 1;
