-- ============================================================
-- V39 模型管理菜单拆分
-- 保留现有 menu:system_settings 的一级位置，将其作为模型管理目录，
-- 并按既有授权关系创建子菜单。系统管理既有菜单不作修改。
-- ============================================================

UPDATE rbac_permission
SET perm_name = '模型管理',
    perm_type = 'CATALOG',
    path = '/models',
    component = NULL,
    visible = 1,
    status = 1
WHERE perm_code = 'menu:system_settings';

INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT 'model:dashboard', '模型仪表盘', 'MENU', p.id, '/models/dashboard', 'DataAnalysis', 10,
       'views/ModelDashboardView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'menu:system_settings'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'model:dashboard');

INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT 'model:providers', '模型服务配置', 'MENU', p.id, '/models/providers', 'Connection', 20,
       'views/ModelProviderConfigView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'menu:system_settings'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'model:providers');

INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT 'model:settings', '模型设置', 'MENU', p.id, '/models/settings', 'SetUp', 30,
       'views/ModelSettingsView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'menu:system_settings'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'model:settings');

INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT 'model:parameters', '模型参数设置', 'MENU', p.id, '/models/parameters', 'Operation', 40,
       'views/ModelParameterSettingsView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'menu:system_settings'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'model:parameters');

INSERT INTO rbac_permission
    (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status)
SELECT 'system:developer-options', '开发者选项', 'MENU', p.id, '/system/developer-options', 'Tools', 50,
       'views/DeveloperOptionsView.vue', 1, 1
FROM rbac_permission p
WHERE p.perm_code = 'rbac:setting'
  AND NOT EXISTS (SELECT 1 FROM rbac_permission WHERE perm_code = 'system:developer-options');

-- 新菜单继承其目录菜单已授予角色的权限与数据范围。
INSERT INTO rbac_role_permission (role_id, permission_id, data_scope)
SELECT source.role_id, target.id, source.data_scope
FROM rbac_role_permission source
JOIN rbac_permission source_menu ON source_menu.id = source.permission_id
JOIN rbac_permission target ON target.perm_code IN (
    'model:dashboard', 'model:providers', 'model:settings', 'model:parameters'
)
WHERE source_menu.perm_code = 'menu:system_settings'
  AND NOT EXISTS (
      SELECT 1 FROM rbac_role_permission existing
      WHERE existing.role_id = source.role_id AND existing.permission_id = target.id
  );

INSERT INTO rbac_role_permission (role_id, permission_id, data_scope)
SELECT source.role_id, target.id, source.data_scope
FROM rbac_role_permission source
JOIN rbac_permission source_menu ON source_menu.id = source.permission_id
JOIN rbac_permission target ON target.perm_code = 'system:developer-options'
WHERE source_menu.perm_code = 'rbac:setting'
  AND NOT EXISTS (
      SELECT 1 FROM rbac_role_permission existing
      WHERE existing.role_id = source.role_id AND existing.permission_id = target.id
  );
