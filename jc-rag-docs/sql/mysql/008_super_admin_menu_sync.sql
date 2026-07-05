-- =====================================================================
-- 008 super_admin 菜单存量补绑 (增量)
-- 作者: JC
-- 说明:
--   1. 005 只给 super_admin 绑定了 API 类型 permCode，未绑定 CATALOG/MENU
--   2. 006 给 admin 角色绑定了 9 个菜单，未给 super_admin 绑定
--   3. 现状: super_admin 用户登录后侧边栏为空 (AuthUserVO.menus=[])
--   4. 修复: 把当前所有 CATALOG/MENU 类型权限补绑到 super_admin 角色
--   5. 幂等: 使用 NOT EXISTS 防止重复插入
-- =====================================================================

START TRANSACTION;

-- 1) 补绑现有 CATALOG/MENU 到 super_admin 角色
INSERT INTO sys_role_permission (role_id, permission_id, data_scope)
SELECT r.id, p.id, 1
FROM sys_role r
JOIN sys_permission p ON p.perm_type IN ('CATALOG', 'MENU')
WHERE r.role_code = 'super_admin'
  AND NOT EXISTS (
        SELECT 1 FROM sys_role_permission rp
         WHERE rp.role_id = r.id AND rp.permission_id = p.id
   );

-- 2) 校验: super_admin 应拥有全部 CATALOG+MENU+API
-- SELECT r.role_code, p.perm_type, COUNT(*) cnt
--   FROM sys_role r
--   JOIN sys_role_permission rp ON rp.role_id = r.id
--   JOIN sys_permission p ON p.id = rp.permission_id
--  WHERE r.role_code = 'super_admin'
--  GROUP BY r.role_code, p.perm_type;

COMMIT;
