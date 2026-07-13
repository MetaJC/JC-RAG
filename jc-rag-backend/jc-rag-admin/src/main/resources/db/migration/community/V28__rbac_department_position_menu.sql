-- =====================================================================
-- V28__rbac_department_position_menu.sql
-- 目的: 给 RBAC 部门/岗位新增菜单 + 权限点, 同步 community + pro 两库
-- 前置: V27 已建立 rbac_department / rbac_position 表
-- 修正: 写入 rbac_permission / rbac_role_permission (RBAC 实际使用的表)
--       旧 sys_permission 无 url_pattern/icon/sort/component/visible 字段, 故不再写 sys_*
--       super_admin 角色位于 rbac_role (id=4)
-- 幂等: INSERT ... NOT EXISTS 风格, 重复执行不会破坏现有菜单
-- =====================================================================

-- 1) 部门管理子菜单 (parent = rbac:manage id=1162)
INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT
    'rbac:departments', '部门管理', 'MENU',
    p.id, '/rbac/departments', 'OfficeBuilding', 50,
    'views/RbacDepartmentView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'rbac:manage'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:departments');

-- 2) 岗位管理子菜单
INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT
    'rbac:positions', '岗位管理', 'MENU',
    p.id, '/rbac/positions', 'UserFilled', 60,
    'views/RbacPositionView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'rbac:manage'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:positions');

-- 3) 部门 CRUD 按钮权限
INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:department:create', '新增部门', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:departments'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:department:create');

INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:department:update', '编辑部门', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:departments'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:department:update');

INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:department:delete', '删除部门', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:departments'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:department:delete');

INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:department:status', '切换部门状态', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:departments'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:department:status');

-- 4) 岗位 CRUD 按钮权限
INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:position:create', '新增岗位', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:positions'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:position:create');

INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:position:update', '编辑岗位', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:positions'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:position:update');

INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:position:delete', '删除岗位', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:positions'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:position:delete');

INSERT INTO rbac_permission (perm_code, perm_name, perm_type, parent_id, status)
SELECT 'rbac:position:status', '切换岗位状态', 'BUTTON', p.id, 1
FROM rbac_permission p WHERE p.perm_code = 'rbac:positions'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'rbac:position:status');

-- 5) 给 super_admin 角色绑定所有新权限 (data_scope=1=全部)
INSERT INTO rbac_role_permission (role_id, permission_id, data_scope)
SELECT r.id, p.id, 1
FROM rbac_role r
JOIN rbac_permission p ON p.perm_code IN (
    'rbac:departments', 'rbac:positions',
    'rbac:department:create', 'rbac:department:update', 'rbac:department:delete', 'rbac:department:status',
    'rbac:position:create', 'rbac:position:update', 'rbac:position:delete', 'rbac:position:status'
)
WHERE r.role_code = 'super_admin'
  AND NOT EXISTS (
    SELECT 1 FROM rbac_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 6) 给 admin 角色绑定部门/岗位菜单 (只读视图, 无按钮权限)
INSERT INTO rbac_role_permission (role_id, permission_id, data_scope)
SELECT r.id, p.id, 1
FROM rbac_role r
JOIN rbac_permission p ON p.perm_code IN ('rbac:departments', 'rbac:positions')
WHERE r.role_code = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM rbac_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
