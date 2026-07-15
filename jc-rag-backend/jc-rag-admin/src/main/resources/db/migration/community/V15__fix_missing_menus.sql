-- =====================================================================
-- 015: 修复核心菜单缺失问题
-- <p>问题分析：
--   1. 日志管理、Agent管理、演示模式配置等菜单可能未正确配置
--   2. 数据库中菜单的 path 必须与前端路由完全一致
--   3. 确保这些菜单对 super_admin 角色可见
--
-- @author JC
-- @date 2026-06-20
-- =====================================================================

-- 1. 确保核心菜单存在并启用
INSERT INTO sys_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
VALUES
    ('menu:system_settings',  '系统设置',     'MENU', 0, '/system/settings',  'Setting',    1, 'views/SystemSettingsView.vue',  1, 1),
    ('menu:knowledge_base',   '知识库管理',   'MENU', 0, '/knowledge/base',    'Folder',     2, 'views/KnowledgeBaseView.vue',   1, 1),
    ('menu:qa_center',        '问答中心',     'MENU', 0, '/qa/center',         'ChatDotRound',3, 'views/ChatCenterView.vue',      1, 1),
    ('menu:audit_logs',       '日志审计',     'MENU', 0, '/audit/logs',        'Document',   4, 'views/AuditLogView.vue',        1, 1),
    ('menu:rbac_manage',      '权限管理',     'CATALOG', 0, '/rbac/manage',       'User',       5, 'views/RbacManageView.vue',      1, 1),
    ('menu:prompt_manage',    '提示词管理',   'MENU', 0, '/prompts/manage',    'Memo',       6, 'views/PromptManageView.vue',    1, 1),
    ('menu:agent_manage',     'Agent与Skill', 'MENU', 0, '/agents/manage',     'MagicStick', 7, 'views/AgentSkillManageView.vue',1, 1),
    ('menu:demo_config',      '演示模式',     'MENU', 0, '/demo/config',       'VideoPlay',  8, 'views/DemoConfigView.vue',      1, 1),
    ('menu:data_permissions', '数据权限',     'MENU', 0, '/data/permissions',  'Lock',       9, 'views/DataPermissionView.vue',  1, 1)
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    icon = VALUES(icon),
    sort = VALUES(sort),
    component = VALUES(component),
    visible = 1,
    status = 1;

-- 2. 确保 RBAC 子菜单存在并启用
-- 说明: MySQL 8 不允许 INSERT...ON DUPLICATE KEY UPDATE 同时 SELECT 同一张表,
--   先用变量取出 rbac_manage 的 id, 再 INSERT, 避免 "can't specify target table for update in FROM clause"
SELECT id INTO @rbac_manage_id FROM sys_permission WHERE perm_code = 'menu:rbac_manage' LIMIT 1;

INSERT INTO sys_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
VALUES
    ('sys:rbac:users',         '用户管理', 'MENU', @rbac_manage_id, '/rbac/users',         'User',        1, 'views/RbacUserView.vue',        1, 1),
    ('sys:rbac:roles',         '角色管理', 'MENU', @rbac_manage_id, '/rbac/roles',         'UserFilled',  2, 'views/RbacRoleView.vue',        1, 1),
    ('sys:rbac:menus',         '菜单管理', 'MENU', @rbac_manage_id, '/rbac/menus',         'Menu',        3, 'views/RbacMenuView.vue',        1, 1),
    ('sys:rbac:permissions',   '权限管理', 'MENU', @rbac_manage_id, '/rbac/permissions',   'Key',         4, 'views/RbacPermissionView.vue',  1, 1)
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    icon = VALUES(icon),
    sort = VALUES(sort),
    component = VALUES(component),
    visible = 1,
    status = 1;

-- 3. 确保 super_admin 角色绑定所有菜单
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.perm_type IN ('MENU', 'CATALOG')
 WHERE r.role_code = 'super_admin'
   AND NOT EXISTS (
       SELECT 1 FROM sys_role_permission rp
        WHERE rp.role_id = r.id AND rp.permission_id = p.id
   );

-- 4. 验证结果
SELECT id, perm_code, perm_name, parent_id, path, visible, status, component
  FROM sys_permission
 WHERE perm_type IN ('MENU', 'CATALOG')
 ORDER BY parent_id, sort, id;

-- 5. 验证 super_admin 权限绑定
SELECT COUNT(*) as menu_count
  FROM sys_role_permission rp
  JOIN sys_role r ON rp.role_id = r.id
  JOIN sys_permission p ON rp.permission_id = p.id
 WHERE r.role_code = 'super_admin'
   AND p.perm_type IN ('MENU', 'CATALOG');