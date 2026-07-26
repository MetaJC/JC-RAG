-- =====================================================================
-- 037: 拆分 "Agent 与 Skill" 单页面菜单为两个独立页面
-- <p>背景:
--   1. 旧页面 AgentSkillManageView.vue 拆分为 RagSkillManageView.vue + RagAgentManageView.vue
--   2. 旧菜单 menu:agent_manage (path=/agents/manage) 禁用, 改路由 redirect
--   3. 新增 menu:rag_skill (path=/rag/skill) 和 menu:rag_agent (path=/rag/agent)
-- <p>操作:
--   1) 禁用旧 menu:agent_manage
--   2) 新增 menu:rag_skill (Skills 管理)
--   3) 新增 menu:rag_agent (Agent 管理)
-- <p>幂等: ON DUPLICATE KEY UPDATE 保证重跑安全
-- @author JC
-- =====================================================================

-- 1) 禁用旧菜单
UPDATE rbac_permission
   SET status = 0,
       visible = 0
 WHERE perm_code = 'menu:agent_manage';

-- 2) 新增 Skills 管理菜单 (替换旧 sort=7)
INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, url_pattern, method, icon, sort, component, visible, status)
VALUES
    ('menu:rag_skill', '技能管理', 'MENU', 0, '/rag/skill', NULL, NULL, 'MagicStick', 7, 'views/RagSkillManageView.vue', 1, 1)
ON DUPLICATE KEY UPDATE
    perm_name   = VALUES(perm_name),
    path        = VALUES(path),
    component   = VALUES(component),
    icon        = VALUES(icon),
    sort        = VALUES(sort),
    visible     = VALUES(visible),
    status      = VALUES(status);

-- 3) 新增 Agent 管理菜单 (sort=8, 接续旧 sort=7 之后)
INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, url_pattern, method, icon, sort, component, visible, status)
VALUES
    ('menu:rag_agent', '智能体管理', 'MENU', 0, '/rag/agent', NULL, NULL, 'Avatar', 8, 'views/RagAgentManageView.vue', 1, 1)
ON DUPLICATE KEY UPDATE
    perm_name   = VALUES(perm_name),
    path        = VALUES(path),
    component   = VALUES(component),
    icon        = VALUES(icon),
    sort        = VALUES(sort),
    visible     = VALUES(visible),
    status      = VALUES(status);
