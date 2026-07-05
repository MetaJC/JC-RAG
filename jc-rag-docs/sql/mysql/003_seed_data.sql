-- ============================================================
-- JC RAG 系统 - 完整种子数据初始化
-- 在 001_jc_rag_all_in_one.sql 和 002_modules.sql 之后执行
-- ============================================================

-- 初始化用户（developer 兜底、admin 超级管理员、普通用户）
INSERT IGNORE INTO sys_user (username, password_hash, nickname, user_type, status, email) VALUES
('developer', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYN76yO', '开发者', 'DEVELOPER', 1, 'developer@jc-rag.local'),
('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYN76yO', '系统管理员', 'ADMIN', 1, 'admin@jc-rag.local'),
('user01', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYN76yO', '张参谋', 'NORMAL', 1, 'user01@jc-rag.local'),
('user02', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYN76yO', '李分析员', 'NORMAL', 1, 'user02@jc-rag.local');

-- 初始化角色
INSERT IGNORE INTO sys_role (role_code, role_name, role_desc) VALUES
('super_admin', '超级管理员', '系统最高权限角色，可管理除 developer 兜底账号外的全部资源'),
('admin', '系统管理员', '负责日常后台运维、配置和审计管理'),
('analyst', '情报分析员', '可执行知识检索、问答分析和文档研判'),
('viewer', '只读观察员', '仅允许查看结果和只读浏览业务数据');

-- 初始化权限
INSERT IGNORE INTO sys_permission (perm_code, perm_name, perm_type, path, method) VALUES
('admin:dashboard', '仪表盘查看', 'MENU', '/admin/dashboard', 'GET'),
('admin:prompts', '提示词管理', 'MENU', '/admin/prompts', 'GET'),
('admin:prompts:edit', '提示词编辑', 'BUTTON', NULL, NULL),
('admin:agents', 'Agent管理', 'MENU', '/admin/agents', 'GET'),
('admin:agents:edit', 'Agent编辑', 'BUTTON', NULL, NULL),
('admin:users', '用户管理', 'MENU', '/admin/users', 'GET'),
('admin:users:edit', '用户编辑', 'BUTTON', NULL, NULL),
('admin:users:delete', '用户删除', 'BUTTON', NULL, NULL),
('admin:demo-config', '演示模式配置', 'MENU', '/admin/demo-config', 'GET'),
('admin:data-permissions', '数据权限', 'MENU', '/admin/data-permissions', 'GET'),
('admin:knowledge-bases', '知识库管理', 'MENU', '/admin/knowledge-bases', 'GET'),
('admin:roles:manage', '角色管理', 'MENU', '/admin/roles', 'GET'),
('admin:permissions:manage', '权限管理', 'MENU', '/admin/permissions', 'GET'),
('user:chat', '问答功能', 'API', '/api/chat/**', NULL),
('user:api-keys', 'APIKey管理', 'API', '/api/user/api-keys/**', NULL),
('user:profile', '个人中心', 'API', '/api/user/profile/**', NULL);

-- 管理员绑定超级管理员角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='admin' AND r.role_code='super_admin';

-- user01 绑定分析员角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='user01' AND r.role_code='analyst';

-- 超级管理员角色绑定所有权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.role_code='super_admin';

-- 初始化系统提示词
INSERT IGNORE INTO sys_prompts (prompt_key, prompt_name, prompt_content, scope, status, description) VALUES
('chat_qa', '问答提示词（默认）', '你是军事领域RAG问答助手。优先基于给定知识片段作答。如果知识片段不足以回答，可以结合常识自由作答，不要输出固定话术。', 'global', 1, '默认RAG问答提示词'),
('chat_general', '通用对话提示词', '直接用简洁的语言回答用户问题。', 'global', 1, '无知识库命中时的通用提示词'),
('agent_default', 'Agent默认提示词', '你是一个智能助手。请根据你的知识和上下文回答用户的问题。', 'global', 1, 'Agent默认系统提示词');

-- 初始化演示模式配置
INSERT IGNORE INTO demo_config (demo_mode_enabled, default_quota_per_day, default_tokens_per_q, default_tokens_per_day, upgrade_hint) VALUES
(0, 20, 2048, 10000, '当日额度已用完。请在个人中心配置您自己的 API Key 以继续使用。');

-- 初始化 Skills
INSERT IGNORE INTO sys_skills (skill_name, skill_key, description, trigger_type, status) VALUES
('通用问答', 'general_qa', '通用问答技能，处理日常问题', 'auto', 1),
('文档分析', 'doc_analysis', '基于上传文档进行分析和总结', 'keyword', 1),
('代码解析', 'code_analysis', '代码审查和分析技能', 'keyword', 1);
