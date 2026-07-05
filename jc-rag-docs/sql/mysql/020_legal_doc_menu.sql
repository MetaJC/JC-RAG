-- =============================================================================
-- 020_legal_doc_menu.sql
-- -----------------------------------------------------------------------------
-- 目的: 管理端 系统设置 下新增 "协议管理" 子菜单, 指向 LegalDocManageView
-- 顺序: 在 017/018 之后执行
-- 兼容: sys_permission / sys_role 已被 018 重命名为 rbac_permission / rbac_role
-- =============================================================================

-- 1. 获取"系统设置"父菜单ID (兼容 sys_permission / rbac_permission)
SET @perm_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_permission') > 0,
    'rbac_permission', 'sys_permission'
);
SET @role_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_role') > 0,
    'rbac_role', 'sys_role'
);

SET @ddl := CONCAT('SELECT id INTO @sys_settings_id FROM ', @perm_table, ' WHERE perm_code = ''menu:system_settings'' LIMIT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 兜底: 如果父菜单不存在则用顶级目录创建
SET @sys_settings_id = COALESCE(@sys_settings_id, 0);

-- 3. 新增 / 更新子菜单 (动态表名)
SET @ddl := CONCAT(
    'INSERT INTO ', @perm_table,
    ' (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status) ',
    'VALUES (''sys:legal-doc:manage'', ''协议管理'', ''MENU'', ?, ''/sys/legal-doc'', ''Document'', 99, ''views/LegalDocManageView.vue'', 1, 1) ',
    'ON DUPLICATE KEY UPDATE ',
    'perm_name=VALUES(perm_name), perm_type=VALUES(perm_type), parent_id=VALUES(parent_id), ',
    'path=VALUES(path), icon=VALUES(icon), sort=VALUES(sort), component=VALUES(component), visible=1, status=1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt USING @sys_settings_id; DEALLOCATE PREPARE stmt;

-- 4. 同步 super_admin / admin 角色的菜单绑定
SET @ddl := CONCAT(
    'INSERT INTO ', @role_table, '_permission (role_id, permission_id) ',
    'SELECT r.id, p.id FROM ', @role_table, ' r JOIN ', @perm_table, ' p ON p.perm_code = ''sys:legal-doc:manage'' ',
    'WHERE r.role_code IN (''super_admin'', ''admin'') ',
    'AND NOT EXISTS (SELECT 1 FROM ', @role_table, '_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 验证
SET @ddl := CONCAT('SELECT id, perm_code, perm_name, parent_id, path, component FROM ', @perm_table,
    ' WHERE perm_code IN (''menu:system_settings'', ''sys:legal-doc:manage'') ORDER BY perm_code');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

