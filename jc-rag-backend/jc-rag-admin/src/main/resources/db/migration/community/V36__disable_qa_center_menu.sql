-- =====================================================================
-- 036: 禁用社区版 admin 后端"问答中心"菜单
-- <p>背景:
--   1. 社区版 admin 端 ChatCenterView.vue / api/chat.ts / 后端 chat/ 目录已删除
--   2. 菜单表实际存于 rbac_permission (前身 sys_permission), 不存在 rbac_menu
--   3. user 端问答中心不受影响, C 端继续可用
-- <p>操作:
--   UPDATE rbac_permission SET status=0, visible=0 WHERE perm_code='menu:qa_center'
--   幂等: 重复执行结果一致
-- @author JC
-- =====================================================================

UPDATE rbac_permission
   SET status = 0,
       visible = 0
 WHERE perm_code = 'menu:qa_center';
