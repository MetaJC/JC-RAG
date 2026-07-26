-- ============================================================
-- V38 继承 Agent 与 Skill 拆分菜单的角色授权
--
-- V37 将旧的 menu:agent_manage 拆分为 menu:rag_skill 和
-- menu:rag_agent。菜单项创建后必须继承原菜单已授予角色的权限，
-- 否则现有用户只能拿到已禁用的旧菜单，侧栏不会显示新菜单。
-- ============================================================

INSERT INTO rbac_role_permission (role_id, permission_id, data_scope)
SELECT legacy.role_id, target.id, legacy.data_scope
FROM rbac_role_permission legacy
JOIN rbac_permission legacy_menu
  ON legacy_menu.id = legacy.permission_id
JOIN rbac_permission target
  ON target.perm_code IN ('menu:rag_skill', 'menu:rag_agent')
WHERE legacy_menu.perm_code = 'menu:agent_manage'
  AND NOT EXISTS (
    SELECT 1
    FROM rbac_role_permission existing
    WHERE existing.role_id = legacy.role_id
      AND existing.permission_id = target.id
  );
