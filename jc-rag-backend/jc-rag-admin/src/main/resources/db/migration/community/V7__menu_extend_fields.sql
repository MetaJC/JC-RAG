-- =====================================================================
-- 007 菜单扩展字段 (增量)
-- 作者: JC
-- 说明:
--   sys_permission 补 3 个前端菜单 dialog 必填字段:
--     is_external TINYINT(1)  是否外链 (0=否 1=是)，仅菜单类型有效
--     keep_alive   TINYINT(1)  是否缓存 (0=否 1=是)，对应前端 keep-alive
--     query        VARCHAR(255) 路由参数, 例 id=1&type=2
--   已有数据默认值: 0 / 0 / NULL
--   SQL 可重复执行 (列存在则跳过)
-- =====================================================================

DELIMITER $$
DROP PROCEDURE IF EXISTS patch_007 $$
CREATE PROCEDURE patch_007()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'is_external') THEN
        ALTER TABLE sys_permission ADD COLUMN is_external TINYINT(1) NOT NULL DEFAULT 0 AFTER visible;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'keep_alive') THEN
        ALTER TABLE sys_permission ADD COLUMN keep_alive TINYINT(1) NOT NULL DEFAULT 0 AFTER is_external;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'query') THEN
        ALTER TABLE sys_permission ADD COLUMN query VARCHAR(255) NULL AFTER keep_alive;
    END IF;
END $$
DELIMITER ;

CALL patch_007();
DROP PROCEDURE patch_007;
