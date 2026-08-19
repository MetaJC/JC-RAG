-- =============================================================================
-- V41__sys_settings_page.sql
-- -----------------------------------------------------------------------------
-- 目的: 系统管理下新增"系统设置"页 (基础信息 + 账号设置), 并播种 sys_config 默认值
-- 顺序: 在 V40 之后执行
-- 依赖: sys_config 表 (V1), rbac_permission / rbac_role_permission (V18 改名后)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. sys_config 播种默认值 (幂等: INSERT IGNORE)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES ('SITE', 'site.system_name', 'JC RAG', 'STRING', 0, '系统名称 (用户端登录页等展示)');

INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES ('SITE', 'site.logo_url', '', 'STRING', 0, '系统 Logo URL (空=使用默认)');

INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES ('SITE', 'site.copyright', '', 'STRING', 0, '版权信息');

INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES ('AUTH', 'auth.register.open', '1', 'BOOLEAN', 0, '是否开放用户注册 (1=开放, 0=关闭; 关闭后注册接口与短信自动注册均不可用)');

INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES ('AUTH', 'auth.login.agree_required', '1', 'BOOLEAN', 0, '登录是否必须勾选用户协议 (1=必须勾选, 0=隐藏协议复选框且不限制)');

-- -----------------------------------------------------------------------------
-- 2. 新增"系统设置"菜单, 挂在系统管理目录 (rbac:setting) 下
-- -----------------------------------------------------------------------------
INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT 'menu:sys_settings', '系统设置', 'MENU', p.id, '/system/preferences', 'Setting', 60,
       'views/SystemPreferencesView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'rbac:setting'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'menu:sys_settings');

-- -----------------------------------------------------------------------------
-- 3. 新菜单继承系统管理目录已授予角色的权限与数据范围
-- -----------------------------------------------------------------------------
INSERT INTO rbac_role_permission (role_id, permission_id, data_scope)
SELECT source.role_id, target.id, source.data_scope
FROM rbac_role_permission source
JOIN rbac_permission source_menu ON source_menu.id = source.permission_id
JOIN rbac_permission target ON target.perm_code = 'menu:sys_settings'
WHERE source_menu.perm_code = 'rbac:setting'
  AND NOT EXISTS (
      SELECT 1 FROM rbac_role_permission existing
      WHERE existing.role_id = source.role_id AND existing.permission_id = target.id
  );
