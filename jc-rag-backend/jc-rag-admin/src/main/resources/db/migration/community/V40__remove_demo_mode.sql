-- =====================================================================
-- 040: 社区版移除演示模式 (功能已迁移至商业版 Pro)
-- <p>背景:
--   1. 演示模式配置/角色配额/用户配额全部逻辑已迁移至商业版
--      (jc_rag_pro 库 pro_demo_config / pro_demo_role_quota / pro_demo_usage)
--   2. 社区版后端 SysDemoModeController / UserQuotaController 及对应
--      Mapper/实体已删除, 前端 DemoConfigView.vue 及路由已删除
-- <p>操作:
--   1. 删除演示模式菜单及其角色授权 (rbac_role_permission + rbac_permission)
--   2. 删除历史遗留权限码 admin:demo-config
--   3. DROP 演示模式三张表 (旧库若未建表则忽略)
--   幂等: 重复执行结果一致
-- @author JC
-- =====================================================================

-- 1. 删除菜单的角色授权绑定
DELETE rp FROM rbac_role_permission rp
INNER JOIN rbac_permission p ON p.id = rp.permission_id
WHERE p.perm_code IN ('menu:demo_config', 'admin:demo-config');

-- 2. 删除菜单/权限记录
DELETE FROM rbac_permission
WHERE perm_code IN ('menu:demo_config', 'admin:demo-config');

-- 3. DROP 演示模式三张表 (IF EXISTS 保证幂等)
DROP TABLE IF EXISTS user_quota_usage;
DROP TABLE IF EXISTS role_quota_limits;
DROP TABLE IF EXISTS demo_config;
