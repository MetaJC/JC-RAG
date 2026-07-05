-- =============================================================================
-- 027_pro_announcement_menu.sql
-- -----------------------------------------------------------------------------
-- 目的: 管理端新增 "Pro 商业版" 顶级菜单, 含 "公告管理" 子菜单, 指向 ProAnnouncementManageView
-- 顺序: 在 020 之后执行 (任意 rbac_permission 已存在即可)
-- 依赖: rbac_permission / rbac_role (兼容 sys_permission / sys_role)
-- =============================================================================

-- 1. 动态表名 (兼容 rbac_ / sys_)
SET @perm_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_permission') > 0,
    'rbac_permission', 'sys_permission'
);
SET @role_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_role') > 0,
    'rbac_role', 'sys_role'
);

-- 2. 创建 / 更新顶级菜单 "Pro 商业版"
SET @ddl := CONCAT(
    'INSERT INTO ', @perm_table,
    ' (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status) ',
    'VALUES (''menu:pro_top'', ''Pro 商业版'', ''MENU'', 0, ''/pro/announcement'', ''Star'', 999, ''views/ProAnnouncementManageView.vue'', 1, 1) ',
    'ON DUPLICATE KEY UPDATE ',
    'perm_name=VALUES(perm_name), perm_type=VALUES(perm_type), parent_id=VALUES(parent_id), ',
    'path=VALUES(path), icon=VALUES(icon), sort=VALUES(sort), component=VALUES(component), visible=1, status=1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 取顶级菜单 ID
SET @ddl := CONCAT('SELECT id INTO @pro_top_id FROM ', @perm_table, ' WHERE perm_code = ''menu:pro_top'' LIMIT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 创建 / 更新子菜单 "公告管理 (Pro)"
SET @ddl := CONCAT(
    'INSERT INTO ', @perm_table,
    ' (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status) ',
    'VALUES (''pro:announcement:manage'', ''公告管理 (Pro)'', ''MENU'', ?, ''/pro/announcement'', ''Bell'', 1, ''views/ProAnnouncementManageView.vue'', 1, 1) ',
    'ON DUPLICATE KEY UPDATE ',
    'perm_name=VALUES(perm_name), perm_type=VALUES(perm_type), parent_id=VALUES(parent_id), ',
    'path=VALUES(path), icon=VALUES(icon), sort=VALUES(sort), component=VALUES(component), visible=1, status=1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; USING @pro_top_id; DEALLOCATE PREPARE stmt;

-- 5. 同步 super_admin / admin 角色的菜单绑定
SET @ddl := CONCAT(
    'INSERT INTO ', @role_table, '_permission (role_id, permission_id) ',
    'SELECT r.id, p.id FROM ', @role_table, ' r JOIN ', @perm_table, ' p ON p.perm_code IN (''menu:pro_top'', ''pro:announcement:manage'') ',
    'WHERE r.role_code IN (''super_admin'', ''admin'') ',
    'AND NOT EXISTS (SELECT 1 FROM ', @role_table, '_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 验证
SET @ddl := CONCAT('SELECT id, perm_code, perm_name, parent_id, path, component FROM ', @perm_table,
    ' WHERE perm_code IN (''menu:pro_top'', ''pro:announcement:manage'') ORDER BY perm_code');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
