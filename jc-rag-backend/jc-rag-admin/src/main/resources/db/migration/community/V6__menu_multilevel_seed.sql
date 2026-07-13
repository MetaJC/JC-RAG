-- =====================================================================
-- 006 多级菜单支持 (增量)
-- 作者: JC
-- 说明:
--   1. sys_permission 补 icon/sort/component/visible 字段
--   2. 现有 9 个硬编码菜单迁入 sys_permission (parentId 全部为 0 即顶级)
--   3. permType 全部为 MENU
--   4. 现有 admin 角色 (角色编码 = 'admin') 自动绑定全部菜单
--   5. SQL 可重复执行 (DELETE 限定在本次新增的 permCode 范围)
-- =====================================================================

-- 1) 字段补齐
ALTER TABLE sys_permission
    ADD COLUMN icon      VARCHAR(64)  NULL COMMENT 'Element Plus 图标名' AFTER method,
    ADD COLUMN sort      INT          NULL DEFAULT 0 COMMENT '排序，越小越靠前' AFTER icon,
    ADD COLUMN component VARCHAR(255) NULL COMMENT '前端组件路径 (views/...vue)' AFTER sort,
    ADD COLUMN visible   TINYINT      NULL DEFAULT 1 COMMENT '0=隐藏 1=显示' AFTER component;

-- 2) 清理本次 seed 涉及的 9 条菜单记录 (permCode 在白名单)
DELETE FROM sys_permission
 WHERE perm_type = 'MENU'
   AND perm_code IN (
        'menu:system_settings', 'menu:knowledge_base', 'menu:qa_center',
        'menu:audit_logs', 'menu:rbac_manage', 'menu:prompt_manage',
        'menu:agent_manage', 'menu:demo_config', 'menu:data_permissions'
   );

-- 3) 写入 9 条顶级菜单 (与 MainLayout 硬编码兜底顺序一致)
INSERT INTO sys_permission
    (perm_code, perm_name, perm_type, parent_id, path, url_pattern, method, icon, sort, component, visible, status)
VALUES
    ('menu:system_settings',  '系统设置',     'MENU', 0, '/system/settings',  NULL, NULL, 'Setting',   1, 'views/SystemSettingsView.vue',  1, 1),
    ('menu:knowledge_base',   '知识库管理',   'MENU', 0, '/knowledge/base',    NULL, NULL, 'Folder',    2, 'views/KnowledgeBaseView.vue',    1, 1),
    ('menu:qa_center',        '问答中心',     'MENU', 0, '/qa/center',         NULL, NULL, 'ChatDotRound', 3, 'views/ChatCenterView.vue',   1, 1),
    ('menu:audit_logs',       '日志审计',     'MENU', 0, '/audit/logs',        NULL, NULL, 'Document',   4, 'views/AuditLogView.vue',        1, 1),
    ('menu:rbac_manage',      '权限管理',     'MENU', 0, '/rbac/manage',       NULL, NULL, 'User',       5, 'views/RbacManageView.vue',      1, 1),
    ('menu:prompt_manage',    '提示词管理',   'MENU', 0, '/prompts/manage',    NULL, NULL, 'Memo',       6, 'views/PromptManageView.vue',    1, 1),
    ('menu:agent_manage',     'Agent 与 Skill','MENU', 0, '/agents/manage',    NULL, NULL, 'MagicStick', 7, 'views/AgentSkillManageView.vue', 1, 1),
    ('menu:demo_config',      '演示模式',     'MENU', 0, '/demo/config',       NULL, NULL, 'VideoPlay',  8, 'views/DemoConfigView.vue',      1, 1),
    ('menu:data_permissions', '数据权限',     'MENU', 0, '/data/permissions',  NULL, NULL, 'Lock',       9, 'views/DataPermissionView.vue',  1, 1);

-- 4) admin 角色绑定全部菜单 (角色编码 = 'admin'，由 001 种子写入)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.perm_code IN (
        'menu:system_settings', 'menu:knowledge_base', 'menu:qa_center',
        'menu:audit_logs', 'menu:rbac_manage', 'menu:prompt_manage',
        'menu:agent_manage', 'menu:demo_config', 'menu:data_permissions'
  )
 WHERE r.role_code = 'admin'
   AND NOT EXISTS (
        SELECT 1 FROM sys_role_permission rp
         WHERE rp.role_id = r.id AND rp.permission_id = p.id
   );
