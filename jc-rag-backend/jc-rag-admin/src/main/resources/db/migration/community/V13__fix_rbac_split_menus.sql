-- =====================================================================
-- 013: 补建 RBAC 拆分后的 4 条独立菜单 (修复 010 写错 parent_id 的问题)
-- <p>原 010 假设 id=8 是 权限管理 容器, 实际 id=8 是 system:view
-- <p>id=1162 (sys:rbac:manage) 才是真正的权限管理容器
-- <p>补建 sys:rbac:users / :roles / :menus / :permissions 并挂到 1162 下
-- <p>隐藏旧的 user:view (id=11) 避免重复
--
-- @author JC
-- =====================================================================

-- 0. 拿 rbac 容器的实际 id
SELECT id INTO @rbac_parent_id
  FROM sys_permission
 WHERE perm_code = 'sys:rbac:manage' AND perm_type = 'MENU'
 LIMIT 1;

-- 1. 隐藏旧 user:view (单条 Tab 整合菜单, 已被 4 拆分页取代)
UPDATE sys_permission
   SET visible = 0, status = 0
 WHERE perm_code = 'user:view'
   AND perm_type = 'MENU';

-- 2. 补 4 条独立菜单, 父目录 = sys:rbac:manage 实际 id
INSERT INTO sys_permission
  (perm_code, perm_name, perm_type, parent_id, path, component, icon, sort, visible, status, keep_alive, is_external)
VALUES
  ('sys:rbac:users',         '用户管理', 'MENU', @rbac_parent_id, '/rbac/users',         'views/RbacUserView.vue',        'User',        1, 1, 1, 0, 0),
  ('sys:rbac:roles',         '角色管理', 'MENU', @rbac_parent_id, '/rbac/roles',         'views/RbacRoleView.vue',        'UserFilled',  2, 1, 1, 0, 0),
  ('sys:rbac:menus',         '菜单管理', 'MENU', @rbac_parent_id, '/rbac/menus',         'views/RbacMenuView.vue',        'Menu',        3, 1, 1, 0, 0),
  ('sys:rbac:permissions',   '权限管理', 'MENU', @rbac_parent_id, '/rbac/permissions',   'views/RbacPermissionView.vue',  'Key',         4, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE
  perm_name = VALUES(perm_name),
  parent_id = VALUES(parent_id),
  path      = VALUES(path),
  component = VALUES(component),
  icon      = VALUES(icon),
  sort      = VALUES(sort),
  visible   = 1,
  status    = 1;

-- 3. 绑 super_admin 角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_role r
  JOIN sys_permission p ON p.perm_code IN (
    'sys:rbac:users', 'sys:rbac:roles', 'sys:rbac:menus', 'sys:rbac:permissions'
  )
 WHERE r.role_code = 'super_admin'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 4. 验证
SELECT id, perm_code, perm_name, perm_type, parent_id, path, sort, visible, status
  FROM sys_permission
 WHERE perm_code IN (
   'sys:rbac:manage', 'user:view',
   'sys:rbac:users', 'sys:rbac:roles', 'sys:rbac:menus', 'sys:rbac:permissions'
 )
 ORDER BY COALESCE(parent_id, 0), sort, id;

SELECT r.role_code, COUNT(rp.permission_id) AS perms
  FROM sys_role r
  JOIN sys_role_permission rp ON rp.role_id = r.id
  JOIN sys_permission p ON p.id = rp.permission_id
   AND p.perm_code IN (
     'sys:rbac:users', 'sys:rbac:roles', 'sys:rbac:menus', 'sys:rbac:permissions'
   )
 WHERE r.role_code = 'super_admin'
 GROUP BY r.role_code;
