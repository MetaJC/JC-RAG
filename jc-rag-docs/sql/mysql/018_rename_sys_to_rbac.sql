-- =============================================================================
-- 018_rename_sys_to_rbac.sql
-- -----------------------------------------------------------------------------
-- 目的: sys 模块 → rbac 模块 重命名迁移 (仅对已运行过 001~017 的库生效)
-- 顺序: 在 001~017 全部执行完毕之后执行
-- 回滚: 见脚本末尾
-- =============================================================================

-- 1. 关闭外键检查 (防止 RENAME 顺序触发约束检查)
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- 2. RENAME TABLE (依赖顺序: 先子表后主表, 先无依赖后被引用)
RENAME TABLE
    sys_user_role        TO rbac_user_role,
    sys_role_permission  TO rbac_role_permission,
    sys_operation_log    TO rbac_operation_log,
    sys_exception_log    TO rbac_exception_log,
    sys_permission       TO rbac_permission,
    sys_role             TO rbac_role,
    sys_user             TO rbac_user;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- 3. 更新 url_pattern: /api/sys/* → /api/rbac/*
UPDATE rbac_permission SET url_pattern = REPLACE(url_pattern, '/api/sys/', '/api/rbac/')
WHERE url_pattern LIKE '/api/sys/%';

-- 4. 更新 perm_code: sys:* → rbac:* (仅 rbac 域)
UPDATE rbac_permission SET perm_code = REPLACE(perm_code, 'sys:', 'rbac:')
WHERE perm_code LIKE 'sys:%';

-- 4.1 兜底：历史数据可能已经存在 rbac:xxx，本次替换会把 rbac:xxx 变成 rbac:rbac:xxx，需要还原
UPDATE rbac_permission SET perm_code = REPLACE(perm_code, 'rbac:rbac:', 'rbac:')
WHERE perm_code LIKE 'rbac:rbac:%';

-- 5. 验证
SELECT
    (SELECT COUNT(*) FROM rbac_user)            AS rbac_user_count,
    (SELECT COUNT(*) FROM rbac_role)            AS rbac_role_count,
    (SELECT COUNT(*) FROM rbac_permission)      AS rbac_permission_count,
    (SELECT COUNT(*) FROM rbac_user_role)       AS rbac_user_role_count,
    (SELECT COUNT(*) FROM rbac_role_permission) AS rbac_role_permission_count,
    (SELECT COUNT(*) FROM rbac_operation_log)   AS rbac_operation_log_count,
    (SELECT COUNT(*) FROM rbac_exception_log)   AS rbac_exception_log_count;

-- =============================================================================
-- 回滚 (反向) - 出现严重兼容问题时可手动执行
-- =============================================================================
-- SET FOREIGN_KEY_CHECKS = 0;
-- RENAME TABLE
--     rbac_user_role        TO sys_user_role,
--     rbac_role_permission  TO sys_role_permission,
--     rbac_operation_log    TO sys_operation_log,
--     rbac_exception_log    TO sys_exception_log,
--     rbac_permission       TO sys_permission,
--     rbac_role             TO sys_role,
--     rbac_user             TO sys_user;
-- SET FOREIGN_KEY_CHECKS = 1;
-- UPDATE sys_permission SET url_pattern = REPLACE(url_pattern, '/api/rbac/', '/api/sys/')
--   WHERE url_pattern LIKE '/api/rbac/%';
-- UPDATE sys_permission SET perm_code = REPLACE(perm_code, 'rbac:', 'sys:')
--   WHERE perm_code LIKE 'rbac:%';
