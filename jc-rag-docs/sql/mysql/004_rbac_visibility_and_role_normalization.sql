-- ============================================================
-- RBAC 数据修正脚本
-- 目的：
-- 1. 统一角色编码为小写
-- 2. 统一内置角色中文名称和中文说明
-- 3. 修正 developer 兜底账号基础属性
-- 4. 为页面展示层准备中文状态映射所需的规范数据
-- ============================================================

START TRANSACTION;

-- developer 兜底账号强制修正
UPDATE sys_user
SET nickname = '开发者',
    user_type = 'DEVELOPER',
    status = 1,
    deleted = 0,
    email = 'developer@jc-rag.local'
WHERE username = 'developer';

-- 角色编码统一转为小写
UPDATE sys_role
SET role_code = LOWER(role_code);

-- 内置角色中文名称和说明统一
UPDATE sys_role
SET role_name = '超级管理员',
    role_desc = '系统最高权限角色，可管理除 developer 兜底账号外的全部资源',
    status = 1
WHERE role_code = 'super_admin';

UPDATE sys_role
SET role_name = '系统管理员',
    role_desc = '负责日常后台运维、配置和审计管理',
    status = 1
WHERE role_code = 'admin';

UPDATE sys_role
SET role_name = '情报分析员',
    role_desc = '可执行知识检索、问答分析和文档研判',
    status = 1
WHERE role_code = 'analyst';

UPDATE sys_role
SET role_name = '只读观察员',
    role_desc = '仅允许查看结果和只读浏览业务数据',
    status = 1
WHERE role_code = 'viewer';

-- 若历史环境中仍残留 USER/GUEST 之类旧角色，也统一转中文
UPDATE sys_role
SET role_name = '普通用户',
    role_desc = '标准业务访问权限'
WHERE role_code = 'user';

UPDATE sys_role
SET role_name = '访客',
    role_desc = '只读受限访问权限'
WHERE role_code = 'guest';

COMMIT;
