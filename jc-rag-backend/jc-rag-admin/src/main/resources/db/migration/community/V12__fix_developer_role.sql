-- =====================================================================
-- 012: 修复 developer 兜底账号无角色问题
-- <p>症状: 用 developer 登录后左侧菜单完全空白, 一项都没有
-- <p>根因: developer 用户 user_type='DEVELOPER' 但 sys_user_role 没有任何绑定
-- <p>      → MenuServiceImpl.listMenuTreeByUserId 在 permCodes 为空时返回空列表
-- <p>      → 前端拿不到任何菜单
-- <p>解决: 给 developer 绑 super_admin 角色 (id=4, 含 30 个菜单+按钮权限)
-- <p>注:   developer 保留为兜底账号, 仅供开发/调试使用, 见 ARCHITECTURE.md
-- <p>注:   权限变更需重新登录才生效 (DbUserDetails 登录时一次性加载)
--
-- @author JC
-- =====================================================================

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
  FROM `sys_user` u
  JOIN `sys_role` r ON r.role_code = 'super_admin'
 WHERE u.username = 'developer'
   AND NOT EXISTS (
       SELECT 1 FROM `sys_user_role` ur
        WHERE ur.user_id = u.id AND ur.role_id = r.id
   );

-- 验证
SELECT u.id, u.username, u.user_type, GROUP_CONCAT(r.role_code) AS roles
  FROM `sys_user` u
  LEFT JOIN `sys_user_role` ur ON ur.user_id = u.id
  LEFT JOIN `sys_role` r ON r.id = ur.role_id
 WHERE u.username IN ('admin', 'developer')
 GROUP BY u.id;
