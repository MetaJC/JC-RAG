-- =====================================================================
-- 017: 彻底修复菜单配置 - 确保所有核心菜单完整显示
-- <p>问题：菜单不完整，缺少知识库管理、Agent管理、演示模式配置等
-- <p>解决方案：重新初始化所有菜单，确保配置正确
--
-- @author JC
-- @date 2026-06-20
-- =====================================================================

-- 1. 创建存储过程：获取或创建菜单项
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS upsert_menu(
    IN p_perm_code VARCHAR(64),
    IN p_perm_name VARCHAR(64),
    IN p_perm_type VARCHAR(16),
    IN p_parent_id BIGINT,
    IN p_path VARCHAR(255),
    IN p_icon VARCHAR(64),
    IN p_sort INT,
    IN p_component VARCHAR(255)
)
BEGIN
    INSERT INTO sys_permission
        (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
    VALUES
        (p_perm_code, p_perm_name, p_perm_type, p_parent_id, p_path, p_icon, p_sort, p_component, 1, 1)
    ON DUPLICATE KEY UPDATE
        perm_name = VALUES(perm_name),
        perm_type = VALUES(perm_type),
        parent_id = VALUES(parent_id),
        path = VALUES(path),
        icon = VALUES(icon),
        sort = VALUES(sort),
        component = VALUES(component),
        visible = 1,
        status = 1;
END$$
DELIMITER ;

-- 2. 获取权限管理目录的ID（用于子菜单绑定）
SELECT id INTO @rbac_parent_id FROM sys_permission WHERE perm_code = 'menu:rbac_manage';

-- 3. 初始化所有顶级菜单
CALL upsert_menu('menu:system_settings',  '系统设置',     'MENU', 0, '/system/settings',  'Setting',    1, 'views/SystemSettingsView.vue');
CALL upsert_menu('menu:knowledge_base',   '知识库管理',   'MENU', 0, '/knowledge/base',    'Folder',     2, 'views/KnowledgeBaseView.vue');
CALL upsert_menu('menu:qa_center',        '问答中心',     'MENU', 0, '/qa/center',         'ChatDotRound',3, 'views/ChatCenterView.vue');
CALL upsert_menu('menu:audit_logs',       '日志审计',     'MENU', 0, '/audit/logs',        'Document',   4, 'views/AuditLogView.vue');
CALL upsert_menu('menu:rbac_manage',      '权限管理',     'CATALOG', 0, '/rbac/manage',       'User',       5, 'views/RbacManageView.vue');
CALL upsert_menu('menu:prompt_manage',    '提示词管理',   'MENU', 0, '/prompts/manage',    'Memo',       6, 'views/PromptManageView.vue');
CALL upsert_menu('menu:agent_manage',     'Agent与Skill', 'MENU', 0, '/agents/manage',     'MagicStick', 7, 'views/AgentSkillManageView.vue');
CALL upsert_menu('menu:demo_config',      '演示模式',     'MENU', 0, '/demo/config',       'VideoPlay',  8, 'views/DemoConfigView.vue');
CALL upsert_menu('menu:data_permissions', '数据权限',     'MENU', 0, '/data/permissions',  'Lock',       9, 'views/DataPermissionView.vue');

-- 4. 重新获取权限管理目录ID（确保存在）
SELECT COALESCE(@rbac_parent_id, (SELECT id FROM sys_permission WHERE perm_code = 'menu:rbac_manage')) INTO @rbac_parent_id;

-- 5. 初始化RBAC子菜单
CALL upsert_menu('sys:rbac:users',         '用户管理', 'MENU', @rbac_parent_id, '/rbac/users',         'User',        1, 'views/RbacUserView.vue');
CALL upsert_menu('sys:rbac:roles',         '角色管理', 'MENU', @rbac_parent_id, '/rbac/roles',         'UserFilled',  2, 'views/RbacRoleView.vue');
CALL upsert_menu('sys:rbac:menus',         '菜单管理', 'MENU', @rbac_parent_id, '/rbac/menus',         'Menu',        3, 'views/RbacMenuView.vue');
CALL upsert_menu('sys:rbac:permissions',   '权限管理', 'MENU', @rbac_parent_id, '/rbac/permissions',   'Key',         4, 'views/RbacPermissionView.vue');

-- 6. 确保super_admin角色绑定所有菜单
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.perm_type IN ('MENU', 'CATALOG')
 WHERE r.role_code = 'super_admin'
   AND NOT EXISTS (
       SELECT 1 FROM sys_role_permission rp
        WHERE rp.role_id = r.id AND rp.permission_id = p.id
   );

-- 7. 清理存储过程
DROP PROCEDURE IF EXISTS upsert_menu;

-- 8. 验证结果
SELECT id, perm_code, perm_name, parent_id, path, visible, status, sort
  FROM sys_permission
 WHERE perm_type IN ('MENU', 'CATALOG')
 ORDER BY parent_id, sort, id;