-- =====================================================================
-- 038: 绑定 super_admin 角色到新拆分的菜单 (rag_skill / rag_agent)
-- <p>背景:
--   1. V37 拆分了 Agent 与 Skill 菜单, 但 super_admin 角色未自动绑定新菜单
--   2. super_admin 角色无新菜单权限, 前端 SidebarMenu 看不到
-- <p>操作:
--   1) 给 super_admin 角色绑定 menu:rag_skill
--   2) 给 super_admin 角色绑定 menu:rag_agent
--   3) 解绑 super_admin 角色的旧 menu:agent_manage (避免显示已禁用的旧菜单)
-- <p>幂等: NOT EXISTS 防止重复绑定
-- @author JC
-- =====================================================================

-- 1) 绑定 super_admin 角色到新菜单
INSERT INTO rbac_role_permission (role_id, permission_id)
SELECT r.id, p.id
  FROM rbac_role r
  JOIN rbac_permission p ON p.perm_code IN ('menu:rag_skill', 'menu:rag_agent')
 WHERE r.role_code = 'super_admin'
   AND NOT EXISTS (
        SELECT 1 FROM rbac_role_permission rp
         WHERE rp.role_id = r.id AND rp.permission_id = p.id
   );

-- 2) 解绑 super_admin 角色的旧 menu:agent_manage (旧菜单已禁用, 避免显示)
DELETE FROM rbac_role_permission
 WHERE permission_id IN (SELECT id FROM rbac_permission WHERE perm_code = 'menu:agent_manage');
