-- =====================================================================
-- 016: 完整修复菜单配置（确保所有核心菜单显示）
-- <p>问题分析：菜单不完整，缺少知识库管理、Agent管理、演示模式等
-- <p>解决方案：强制初始化所有菜单，确保path与前端路由一致
--
-- @author JC
-- @date 2026-06-20
-- =====================================================================

-- 1. 先查看当前菜单状态（诊断）
SELECT id, perm_code, perm_name, perm_type, parent_id, path, visible, status 
  FROM sys_permission 
 WHERE perm_type IN ('MENU', 'CATALOG')
 ORDER BY parent_id, sort, id;

-- 2. 启用所有顶级菜单（确保核心功能可见）
UPDATE sys_permission
   SET visible = 1, status = 1
 WHERE perm_code IN (
   'menu:system_settings', 'menu:knowledge_base', 'menu:qa_center',
   'menu:audit_logs', 'menu:rbac_manage', 'menu:prompt_manage',
   'menu:agent_manage', 'menu:demo_config', 'menu:data_permissions'
 );

-- 3. 启用所有RBAC子菜单
UPDATE sys_permission
   SET visible = 1, status = 1
 WHERE perm_code IN (
   'sys:rbac:users', 'sys:rbac:roles', 'sys:rbac:menus', 'sys:rbac:permissions'
 );

-- 4. 确保顶级菜单的path与前端路由一致
UPDATE sys_permission SET path = '/system/settings'  WHERE perm_code = 'menu:system_settings';
UPDATE sys_permission SET path = '/knowledge/base'   WHERE perm_code = 'menu:knowledge_base';
UPDATE sys_permission SET path = '/qa/center'        WHERE perm_code = 'menu:qa_center';
UPDATE sys_permission SET path = '/audit/logs'       WHERE perm_code = 'menu:audit_logs';
UPDATE sys_permission SET path = '/rbac/manage'      WHERE perm_code = 'menu:rbac_manage';
UPDATE sys_permission SET path = '/prompts/manage'   WHERE perm_code = 'menu:prompt_manage';
UPDATE sys_permission SET path = '/agents/manage'    WHERE perm_code = 'menu:agent_manage';
UPDATE sys_permission SET path = '/demo/config'      WHERE perm_code = 'menu:demo_config';
UPDATE sys_permission SET path = '/data/permissions' WHERE perm_code = 'menu:data_permissions';

-- 5. 确保RBAC子菜单的path与前端路由一致
UPDATE sys_permission SET path = '/rbac/users'         WHERE perm_code = 'sys:rbac:users';
UPDATE sys_permission SET path = '/rbac/roles'         WHERE perm_code = 'sys:rbac:roles';
UPDATE sys_permission SET path = '/rbac/menus'         WHERE perm_code = 'sys:rbac:menus';
UPDATE sys_permission SET path = '/rbac/permissions'   WHERE perm_code = 'sys:rbac:permissions';

-- 6. 确保super_admin角色绑定所有菜单权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.perm_type IN ('MENU', 'CATALOG')
 WHERE r.role_code = 'super_admin'
   AND NOT EXISTS (
       SELECT 1 FROM sys_role_permission rp
        WHERE rp.role_id = r.id AND rp.permission_id = p.id
   );

-- 7. 最终验证
SELECT id, perm_code, perm_name, parent_id, path, visible, status, sort
  FROM sys_permission
 WHERE perm_type IN ('MENU', 'CATALOG')
 ORDER BY parent_id, sort, id;