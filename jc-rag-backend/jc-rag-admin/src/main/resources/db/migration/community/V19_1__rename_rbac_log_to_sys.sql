-- =============================================================================
-- 019_rename_rbac_log_to_sys.sql
-- -----------------------------------------------------------------------------
-- 目的: 日志相关表 rbac_*log → sys_*log 重命名迁移
-- 顺序: 在 018 之后执行 (先把 sys 全部改 rbac, 此次再把日志表改回 sys_*log)
-- 回滚: 见脚本末尾
-- =============================================================================

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

RENAME TABLE
    rbac_operation_log TO sys_operation_log,
    rbac_exception_log TO sys_exception_log;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- 回滚: sys_*log → rbac_*log
-- SET FOREIGN_KEY_CHECKS = 0;
-- RENAME TABLE
--     sys_operation_log TO rbac_operation_log,
--     sys_exception_log TO rbac_exception_log;
-- SET FOREIGN_KEY_CHECKS = 1;
