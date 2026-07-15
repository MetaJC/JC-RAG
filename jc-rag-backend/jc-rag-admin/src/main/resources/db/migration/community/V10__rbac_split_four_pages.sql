-- =====================================================================
-- 010: RBAC 拆 4 页面, 加 4 条独立菜单
-- <p>原 rbac:manage (单条菜单) 拆为: 用户管理 / 角色管理 / 菜单管理 / 权限管理
-- <p>每条独立 perm_code, 可独立授权
-- <p>挂在 `权限管理` (sys:rbac_manage, id=8 旧) 下, 作为子菜单
-- <p>执行前请确认: 数据库里 sys_permission id=8 是 "权限管理" 目录
--
-- @author JC
-- =====================================================================

-- 0. 先隐藏/停用旧 rbac:manage 整合菜单 (避免重复)
UPDATE sys_permission
   SET visible = 0, status = 0, updated_at = NOW()
 WHERE perm_code = 'sys:rbac:manage'
   AND perm_type IN ('CATALOG', 'MENU');

-- 1. 加 4 条独立菜单 (子级挂在 rbac_manage 下, 即 parent_id=8)
-- 1.1 用户管理
INSERT INTO sys_permission
  (perm_code, perm_name, perm_type, parent_id, path, component, icon, sort, visible, status, keep_alive, is_external, created_at, updated_at)
VALUES
  ('sys:rbac:users', '用户管理', 'MENU', 8, '/rbac/users', 'views/RbacUserView.vue', 'User', 1, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  path = VALUES(path), component = VALUES(component), icon = VALUES(icon),
  perm_name = VALUES(perm_name), sort = VALUES(sort), updated_at = NOW();

-- 1.2 角色管理
INSERT INTO sys_permission
  (perm_code, perm_name, perm_type, parent_id, path, component, icon, sort, visible, status, keep_alive, is_external, created_at, updated_at)
VALUES
  ('sys:rbac:roles', '角色管理', 'MENU', 8, '/rbac/roles', 'views/RbacRoleView.vue', 'UserFilled', 2, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  path = VALUES(path), component = VALUES(component), icon = VALUES(icon),
  perm_name = VALUES(perm_name), sort = VALUES(sort), updated_at = NOW();

-- 1.3 菜单管理
INSERT INTO sys_permission
  (perm_code, perm_name, perm_type, parent_id, path, component, icon, sort, visible, status, keep_alive, is_external, created_at, updated_at)
VALUES
  ('sys:rbac:menus', '菜单管理', 'MENU', 8, '/rbac/menus', 'views/RbacMenuView.vue', 'Menu', 3, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  path = VALUES(path), component = VALUES(component), icon = VALUES(icon),
  perm_name = VALUES(perm_name), sort = VALUES(sort), updated_at = NOW();

-- 1.4 权限管理
INSERT INTO sys_permission
  (perm_code, perm_name, perm_type, parent_id, path, component, icon, sort, visible, status, keep_alive, is_external, created_at, updated_at)
VALUES
  ('sys:rbac:permissions', '权限管理', 'MENU', 8, '/rbac/permissions', 'views/RbacPermissionView.vue', 'Key', 4, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  path = VALUES(path), component = VALUES(component), icon = VALUES(icon),
  perm_name = VALUES(perm_name), sort = VALUES(sort), updated_at = NOW();

-- 2. 给 super_admin 角色绑上 4 条新菜单
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
  FROM sys_role r
  JOIN sys_permission p ON p.perm_code IN (
    'sys:rbac:users', 'sys:rbac:roles', 'sys:rbac:menus', 'sys:rbac:permissions'
  )
 WHERE r.role_code = 'super_admin'
ON DUPLICATE KEY UPDATE created_at = NOW();

-- 3. 验证
SELECT id, perm_code, perm_name, perm_type, parent_id, path, sort, visible, status
  FROM sys_permission
 WHERE perm_code IN (
   'sys:rbac:manage', 'sys:rbac:users', 'sys:rbac:roles', 'sys:rbac:menus', 'sys:rbac:permissions'
 )
 ORDER BY perm_code;
