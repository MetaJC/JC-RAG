-- ============================================================
-- RBAC 架构优化 DDL 增量脚本
-- 目的：配合阶段 2.3 / 阶段 3 / 阶段 4 的代码改造，扩充两张表
-- 1. sys_role_permission 加 data_scope：角色数据权限范围（后续 AOP 数据过滤用）
-- 2. sys_permission 加 url_pattern：Controller 后端路径（启用 permCode 路由级鉴权）
-- 兼容性：IF NOT EXISTS 风格补列，MySQL 5.7+ 适用
-- ============================================================

START TRANSACTION;

-- 1) sys_role_permission.data_scope
--    1=全部 2=本部门 3=本人 4=自定义（暂不实现自定义规则解析）
ALTER TABLE sys_role_permission
    ADD COLUMN data_scope TINYINT NOT NULL DEFAULT 1
        COMMENT '数据权限范围: 1=全部 2=本部门 3=本人 4=自定义' AFTER permission_id;

-- 2) sys_permission.url_pattern
--    Controller 注解 path（带占位符，如 /api/sys/user/{id}）
--    PermissionRouteLoader 用此字段构建 (pattern, method) -> permCode 路由表
--    与 path 字段（前端菜单路径）并存：path 用于前端菜单渲染，url_pattern 用于后端鉴权
ALTER TABLE sys_permission
    ADD COLUMN url_pattern VARCHAR(255) NULL
        COMMENT 'Controller 路径（含占位符），用于后端 permCode 路由级鉴权' AFTER path;

-- 3) 索引：url_pattern + method 组合查询
ALTER TABLE sys_permission
    ADD INDEX idx_url_method (url_pattern, method);

-- 4) 录入 sys 模块 API 路由 permCode（供 PermissionRouteLoader 加载）
--    注意：path 字段仍为前端菜单路径；url_pattern 字段为后端 Controller 注解 path
INSERT IGNORE INTO sys_permission (perm_code, perm_name, perm_type, path, url_pattern, method, status) VALUES
-- 用户管理
('rbac:user:list',     '用户列表',   'API', '/rbac/manage', '/api/sys/user',          'GET',    1),
('rbac:user:detail',   '用户详情',   'API', '/rbac/manage', '/api/sys/user/{id}',     'GET',    1),
('rbac:user:create',   '新建用户',   'API', '/rbac/manage', '/api/sys/user',          'POST',   1),
('rbac:user:update',   '更新用户',   'API', '/rbac/manage', '/api/sys/user/{id}',     'PUT',    1),
('rbac:user:delete',   '删除用户',   'API', '/rbac/manage', '/api/sys/user/{id}',     'DELETE', 1),
('rbac:user:resetPwd', '重置密码',   'API', '/rbac/manage', '/api/sys/user/{id}/password', 'PUT', 1),
('rbac:user:toggle',   '启停用户',   'API', '/rbac/manage', '/api/sys/user/{id}/status',    'PUT', 1),
-- 角色管理
('rbac:role:list',     '角色列表',   'API', '/rbac/manage', '/api/sys/role',          'GET',    1),
('rbac:role:create',   '新建角色',   'API', '/rbac/manage', '/api/sys/role',          'POST',   1),
('rbac:role:update',   '更新角色',   'API', '/rbac/manage', '/api/sys/role/{id}',     'PUT',    1),
('rbac:role:delete',   '删除角色',   'API', '/rbac/manage', '/api/sys/role/{id}',     'DELETE', 1),
('rbac:role:perm',     '角色授权',   'API', '/rbac/manage', '/api/sys/role/{id}/permissions', 'PUT', 1),
-- 权限管理
('rbac:perm:list',     '权限列表',   'API', '/rbac/manage', '/api/sys/permission',    'GET',    1),
('rbac:perm:create',   '新建权限',   'API', '/rbac/manage', '/api/sys/permission',    'POST',   1),
('rbac:perm:update',   '更新权限',   'API', '/rbac/manage', '/api/sys/permission/{id}', 'PUT',  1),
('rbac:perm:delete',   '删除权限',   'API', '/rbac/manage', '/api/sys/permission/{id}', 'DELETE', 1),
-- 操作日志
('audit:opLog:list',   '操作日志',   'API', '/audit/logs',  '/api/sys/operation-log', 'GET',    1),
-- 异常日志
('audit:exLog:list',   '异常日志',   'API', '/audit/logs',  '/api/sys/exception-log', 'GET',    1);

-- 5) 为 super_admin 角色绑定全部 permType='API' 的 permCode（保证超级管理员走全量鉴权通道）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, data_scope)
SELECT r.id, p.id, 1
FROM sys_role r, sys_permission p
WHERE r.role_code = 'super_admin' AND p.perm_type = 'API';

COMMIT;

-- ============================================================
-- 执行后校验（不应有错误）
-- ============================================================
-- SHOW COLUMNS FROM sys_role_permission LIKE 'data_scope';
-- SHOW COLUMNS FROM sys_permission LIKE 'url_pattern';
-- SHOW INDEX FROM sys_permission WHERE Key_name = 'idx_url_method';
