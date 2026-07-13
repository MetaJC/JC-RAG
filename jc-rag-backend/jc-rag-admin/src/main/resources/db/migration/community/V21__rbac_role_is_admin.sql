-- =============================================================================
-- 021_rbac_role_is_admin.sql
-- -----------------------------------------------------------------------------
-- 目的: 角色表新增 is_admin 字段, 控制"该角色是否允许登录管理端"
-- 业务规则:
--   1. 默认 0 (不允许登录后台, 普通用户角色一律不勾选)
--   2. 只有 is_admin=1 的角色才被允许登录管理端
--   3. super_admin 角色强制 is_admin=1, 业务层校验禁止改为 0 (developer 账号除外)
-- 顺序: 在 018 (sys_role -> rbac_role) 之后执行
-- 兼容: 兼容 sys_role (老库) 与 rbac_role (新库) 两套表名
-- =============================================================================

-- 1. 检测角色表名 (兼容老库 sys_role / 新库 rbac_role)
SET @role_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_role') > 0,
    'rbac_role', 'sys_role'
);

-- 2. 新增 is_admin 列 (默认 0, 即"不允许登录后台")
SET @ddl := CONCAT(
    'ALTER TABLE ', @role_table,
    ' ADD COLUMN is_admin TINYINT NOT NULL DEFAULT 0 COMMENT ''是否允许登录管理端 (0 否 / 1 是)''',
    ' AFTER status'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 初始化: super_admin 强制 is_admin=1, 其他角色保持 0
SET @ddl := CONCAT(
    'UPDATE ', @role_table, ' SET is_admin = 1 WHERE role_code = ''super_admin'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 验证
SET @ddl := CONCAT(
    'SELECT role_code, role_name, status, is_admin FROM ', @role_table,
    ' ORDER BY id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
