-- =====================================================================
-- 014: 禁用重复菜单路径（仅禁用 /admin/* 系列重复项，保留核心功能）
-- <p>策略：先禁用（visible=0, status=0），运行稳定后再彻底删除
-- <p>保留的标准化路径：/rbac/users, /rbac/roles, /rbac/menus, /rbac/permissions,
--    /knowledge/base, /audit/logs, /demo/config, /data/permissions,
--    /prompts/manage, /agents/manage, /qa/center, /system/settings
-- <p>禁用的重复路径：仅 /admin/* 系列（作为上述路径的别名，已不再使用）
--
-- @author JC
-- @date 2026-06-20
-- =====================================================================

-- 1. 禁用 /admin/* 系列重复菜单（保留 id，标记为隐藏+禁用）
-- 注意：这些是旧路径，对应功能已通过新路径提供
UPDATE sys_permission
   SET visible = 0,
       status = 0,
       updated_at = NOW()
 WHERE perm_type = 'MENU'
   AND path LIKE '/admin/%'
   AND status = 1;

-- 2. 禁用旧的单条整合菜单（user:view, dashboard:view, kb:view）
-- 这些已被 /rbac/users, /system/settings, /knowledge/base 取代
UPDATE sys_permission
   SET visible = 0,
       status = 0,
       updated_at = NOW()
 WHERE perm_code IN ('user:view', 'dashboard:view', 'kb:view')
   AND perm_type = 'MENU';

-- 3. 禁用根路径重复（/dashboard, /users, /knowledge 已被新路径取代）
UPDATE sys_permission
   SET visible = 0,
       status = 0,
       updated_at = NOW()
 WHERE perm_type = 'MENU'
   AND path IN ('/dashboard', '/users', '/knowledge')
   AND status = 1;

-- 4. 确保核心功能菜单保持启用状态（保险操作）
UPDATE sys_permission
   SET visible = 1,
       status = 1,
       updated_at = NOW()
 WHERE perm_type = 'MENU'
   AND path IN (
     '/rbac/users', '/rbac/roles', '/rbac/menus', '/rbac/permissions',
     '/knowledge/base', '/audit/logs', '/demo/config', '/data/permissions',
     '/prompts/manage', '/agents/manage', '/qa/center', '/system/settings'
   );

-- 5. 验证：确认仍在使用的菜单
SELECT id, perm_code, perm_name, perm_type, path, visible, status
  FROM sys_permission
 WHERE perm_type IN ('MENU', 'CATALOG')
   AND status = 1
 ORDER BY COALESCE(parent_id, 0), sort, id;

-- 6. 验证：确认已禁用的菜单
SELECT id, perm_code, perm_name, perm_type, path, visible, status
  FROM sys_permission
 WHERE perm_type = 'MENU'
   AND status = 0
 ORDER BY id;

-- 7. 验证核心功能菜单状态
SELECT id, perm_code, perm_name, path, visible, status
  FROM sys_permission
 WHERE perm_type = 'MENU'
   AND path IN (
     '/knowledge/base', '/audit/logs', '/demo/config',
     '/rbac/users', '/rbac/roles', '/rbac/menus', '/rbac/permissions',
     '/prompts/manage', '/agents/manage', '/data/permissions',
     '/qa/center', '/system/settings'
   )
 ORDER BY path;