-- ============================================================
-- JC RAG 智能知识库系统
-- MySQL 一体化初始化脚本
-- 作用：
-- 1. 创建数据库
-- 2. 创建系统所需全部核心表
-- 3. 初始化 RBAC 基础数据
-- 4. 初始化系统配置、模型配置、RAG 配置
-- 5. 初始化 Agent / Skill 基础数据
--
-- 使用方式：
-- 在 MySQL 中直接执行本脚本即可完成首轮初始化。
-- 适用环境：
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS jc_rag
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE jc_rag;

-- ============================================================
-- 一、认证与权限模块
-- 用途：
-- 1. 存储用户、角色、权限
-- 2. 支撑 RBAC 权限控制
-- 3. 为菜单级、按钮级、接口级权限提供基础数据
-- ============================================================

-- 用户表：存储系统登录用户基本信息
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  user_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  last_login_time DATETIME NULL,
  last_login_ip VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
) COMMENT='系统用户表';

-- 角色表：定义管理员、普通用户、访客等角色
CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  role_desc VARCHAR(255) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='系统角色表';

-- 权限表：统一存储菜单、按钮、接口权限
CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  perm_code VARCHAR(128) NOT NULL UNIQUE,
  perm_name VARCHAR(128) NOT NULL,
  perm_type VARCHAR(32) NOT NULL,
  parent_id BIGINT NULL,
  path VARCHAR(255) NULL,
  method VARCHAR(16) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='系统权限表';

-- 用户角色关联表：一个用户可绑定多个角色
CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT='用户角色关联表';

-- 角色权限关联表：一个角色可绑定多个权限点
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_permission (role_id, permission_id)
) COMMENT='角色权限关联表';

-- ============================================================
-- 二、知识库与文档管理模块
-- 用途：
-- 1. 管理知识库
-- 2. 管理文档元数据、版本、处理任务
-- 3. 为 MinIO 文件、ES 向量数据提供关联主键
-- ============================================================

-- 知识库表：支持多知识库隔离
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kb_code VARCHAR(64) NOT NULL UNIQUE,
  kb_name VARCHAR(128) NOT NULL,
  kb_desc VARCHAR(500) NULL,
  visibility_scope VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
  owner_user_id BIGINT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='知识库定义表';

-- 文档表：存储上传文档的核心元数据
CREATE TABLE IF NOT EXISTS kb_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kb_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(32) NOT NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  content_type VARCHAR(128) NULL,
  minio_bucket VARCHAR(128) NOT NULL,
  minio_object_name VARCHAR(255) NOT NULL,
  file_hash VARCHAR(128) NOT NULL,
  version_no INT NOT NULL DEFAULT 1,
  source_path VARCHAR(500) NULL,
  doc_type VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
  security_level VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
  parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  parse_message VARCHAR(1000) NULL,
  page_count INT NOT NULL DEFAULT 0,
  chunk_count INT NOT NULL DEFAULT 0,
  uploaded_by BIGINT NOT NULL,
  uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_kb_document_kb_id (kb_id),
  KEY idx_kb_document_status (parse_status),
  KEY idx_kb_document_hash (file_hash)
) COMMENT='文档元数据表';

-- 文档版本表：管理文档的历史版本
CREATE TABLE IF NOT EXISTS kb_document_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  minio_object_name VARCHAR(255) NOT NULL,
  file_hash VARCHAR(128) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500) NULL,
  UNIQUE KEY uk_document_version (document_id, version_no)
) COMMENT='文档版本表';

-- 文档处理任务表：记录解析、切块、向量化等异步任务
CREATE TABLE IF NOT EXISTS kb_document_chunk_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  task_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_chunk_task_document (document_id),
  KEY idx_chunk_task_status (task_status)
) COMMENT='文档处理任务表';

-- ============================================================
-- 三、会话与问答模块
-- 用途：
-- 1. 记录用户会话
-- 2. 记录问答消息
-- 3. 记录回答引用来源，支持溯源
-- ============================================================

-- 会话主表：存储每个对话会话的基本信息
CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_title VARCHAR(255) NOT NULL,
  session_mode VARCHAR(32) NOT NULL DEFAULT 'RAG',
  model_mode VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
  model_name VARCHAR(128) NULL,
  kb_id BIGINT NULL,
  is_pinned TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_chat_session_user (user_id),
  KEY idx_chat_session_kb (kb_id)
) COMMENT='问答会话表';

-- 会话消息表：记录用户消息和模型回复
CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role_type VARCHAR(32) NOT NULL,
  message_content LONGTEXT NOT NULL,
  message_index INT NOT NULL,
  token_count INT NOT NULL DEFAULT 0,
  latency_ms BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_chat_message_session (session_id),
  KEY idx_chat_message_created (created_at)
) COMMENT='问答消息表';

-- 消息引用表：记录回答引用的文档片段，支撑前端溯源展示
CREATE TABLE IF NOT EXISTS chat_message_reference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  chunk_id VARCHAR(128) NOT NULL,
  page_no INT NULL,
  section_title VARCHAR(255) NULL,
  score DECIMAL(10,6) NOT NULL DEFAULT 0,
  quote_text TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_message_reference_message (message_id),
  KEY idx_message_reference_document (document_id)
) COMMENT='消息引用来源表';

-- ============================================================
-- 四、配置中心模块
-- 用途：
-- 1. 统一管理系统配置
-- 2. 统一管理在线/离线模型配置
-- 3. 管理 RAG 检索与分块参数
-- ============================================================

-- 通用配置表：保存系统级键值配置
CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_group VARCHAR(64) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value TEXT NOT NULL,
  value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
  encrypted TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_config (config_group, config_key)
) COMMENT='系统配置表';

-- 模型提供方配置表：统一管理在线模型和离线模型
CREATE TABLE IF NOT EXISTS llm_provider_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_type VARCHAR(32) NOT NULL,
  provider_name VARCHAR(64) NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  api_key_encrypted TEXT NULL,
  model_name VARCHAR(128) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  priority_no INT NOT NULL DEFAULT 100,
  extra_config_json JSON NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_llm_provider_type (provider_type),
  KEY idx_llm_provider_enabled (enabled)
) COMMENT='模型提供方配置表';

-- RAG 运行参数表：分块、召回、阈值等参数的统一配置
CREATE TABLE IF NOT EXISTS rag_runtime_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chunk_size INT NOT NULL DEFAULT 700,
  chunk_overlap INT NOT NULL DEFAULT 120,
  top_k INT NOT NULL DEFAULT 5,
  top_n INT NOT NULL DEFAULT 20,
  similarity_threshold DECIMAL(10,4) NOT NULL DEFAULT 0.6000,
  enable_hybrid_search TINYINT NOT NULL DEFAULT 0,
  enable_rerank TINYINT NOT NULL DEFAULT 0,
  history_rounds INT NOT NULL DEFAULT 3,
  embedding_model_path VARCHAR(500) NOT NULL,
  embedding_device VARCHAR(16) NOT NULL DEFAULT 'auto',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='RAG 运行参数表';

-- ============================================================
-- 五、日志审计与监控模块
-- 用途：
-- 1. 记录系统操作日志
-- 2. 记录异常日志
-- 3. 记录 RAG 调用日志
-- 4. 记录服务健康检查历史
-- ============================================================

-- 操作日志表：记录用户在系统中的关键操作
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  module_name VARCHAR(64) NOT NULL,
  operation_type VARCHAR(64) NOT NULL,
  operation_desc VARCHAR(500) NULL,
  request_uri VARCHAR(255) NULL,
  request_method VARCHAR(16) NULL,
  request_ip VARCHAR(64) NULL,
  status VARCHAR(16) NOT NULL,
  cost_ms BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_log_user (user_id),
  KEY idx_operation_log_module (module_name),
  KEY idx_operation_log_created (created_at)
) COMMENT='系统操作日志表';

-- 异常日志表：记录系统运行中的异常堆栈信息
CREATE TABLE IF NOT EXISTS sys_exception_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64) NULL,
  module_name VARCHAR(64) NOT NULL,
  exception_type VARCHAR(128) NOT NULL,
  exception_message VARCHAR(1000) NOT NULL,
  stack_trace LONGTEXT NULL,
  operator_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_exception_log_trace (trace_id),
  KEY idx_exception_log_module (module_name),
  KEY idx_exception_log_created (created_at)
) COMMENT='系统异常日志表';

-- RAG 调用日志表：记录检索、模型调用、耗时、token 使用情况
CREATE TABLE IF NOT EXISTS rag_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  session_id BIGINT NULL,
  query_text TEXT NOT NULL,
  rewritten_query TEXT NULL,
  retrieved_chunks INT NOT NULL DEFAULT 0,
  model_mode VARCHAR(32) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  prompt_tokens INT NOT NULL DEFAULT 0,
  completion_tokens INT NOT NULL DEFAULT 0,
  latency_ms BIGINT NOT NULL DEFAULT 0,
  success_flag TINYINT NOT NULL DEFAULT 1,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rag_call_user (user_id),
  KEY idx_rag_call_session (session_id),
  KEY idx_rag_call_created (created_at)
) COMMENT='RAG 调用日志表';

-- 服务健康检查历史表：记录 ES、MinIO、Redis、Ollama、Python 服务状态
CREATE TABLE IF NOT EXISTS service_health_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(64) NOT NULL,
  service_endpoint VARCHAR(255) NULL,
  health_status VARCHAR(32) NOT NULL,
  response_message VARCHAR(1000) NULL,
  latency_ms BIGINT NOT NULL DEFAULT 0,
  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_service_health_name (service_name),
  KEY idx_service_health_checked_at (checked_at)
) COMMENT='服务健康检查历史表';

-- ============================================================
-- 六、Agent 与 Skill 模块
-- 用途：
-- 1. 记录 Agent 复杂任务执行
-- 2. 记录 Agent 的步骤轨迹
-- 3. 定义可扩展的技能模板
-- ============================================================

-- Agent 任务主表：记录复杂任务的整体执行状态
CREATE TABLE IF NOT EXISTS agent_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  task_name VARCHAR(255) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  task_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  input_text LONGTEXT NOT NULL,
  final_output LONGTEXT NULL,
  total_steps INT NOT NULL DEFAULT 0,
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_agent_task_user (user_id),
  KEY idx_agent_task_session (session_id),
  KEY idx_agent_task_status (task_status)
) COMMENT='Agent 任务主表';

-- Agent 步骤表：记录任务拆解后的每一步执行轨迹
CREATE TABLE IF NOT EXISTS agent_task_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  step_no INT NOT NULL,
  step_name VARCHAR(255) NOT NULL,
  tool_name VARCHAR(128) NULL,
  step_input LONGTEXT NULL,
  step_output LONGTEXT NULL,
  step_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  cost_ms BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_agent_task_step_task (task_id),
  KEY idx_agent_task_step_status (step_status)
) COMMENT='Agent 任务步骤表';

-- Skill 定义表：定义基础技能和军事专项技能模板
CREATE TABLE IF NOT EXISTS skill_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  skill_code VARCHAR(64) NOT NULL UNIQUE,
  skill_name VARCHAR(128) NOT NULL,
  skill_type VARCHAR(64) NOT NULL,
  prompt_template LONGTEXT NOT NULL,
  input_schema_json JSON NULL,
  output_schema_json JSON NULL,
  allowed_tools_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_skill_type (skill_type),
  KEY idx_skill_enabled (enabled)
) COMMENT='Skill 定义表';

-- ============================================================
-- 七、基础数据初始化
-- 用途：
-- 1. 初始化默认角色
-- 2. 初始化默认权限
-- 3. 初始化默认管理员
-- 4. 初始化系统配置与模型配置
-- 5. 初始化基础 Skill
-- ============================================================

-- 初始化默认角色
INSERT INTO sys_role (role_code, role_name, role_desc)
VALUES
  ('admin', '系统管理员', '负责日常后台运维、配置和审计管理'),
  ('user', '普通用户', '标准业务访问权限'),
  ('guest', '访客', '只读受限访问权限')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  role_desc = VALUES(role_desc);

-- 初始化权限点
INSERT INTO sys_permission (perm_code, perm_name, perm_type, parent_id, path, method, status)
VALUES
  ('dashboard:view', 'View Dashboard', 'MENU', NULL, '/dashboard', 'GET', 1),
  ('chat:view', 'View Chat', 'MENU', NULL, '/chat', 'GET', 1),
  ('chat:ask', 'Ask Question', 'API', NULL, '/api/chat/ask', 'POST', 1),
  ('kb:view', 'View Knowledge Base', 'MENU', NULL, '/knowledge', 'GET', 1),
  ('kb:upload', 'Upload Document', 'API', NULL, '/api/documents/upload', 'POST', 1),
  ('kb:delete', 'Delete Document', 'API', NULL, '/api/documents/{id}', 'DELETE', 1),
  ('kb:reindex', 'Rebuild Index', 'API', NULL, '/api/index/rebuild', 'POST', 1),
  ('system:view', 'View System Settings', 'MENU', NULL, '/system', 'GET', 1),
  ('system:config', 'Edit System Settings', 'API', NULL, '/api/system/config', 'PUT', 1),
  ('system:monitor', 'View Service Monitor', 'API', NULL, '/api/system/health', 'GET', 1),
  ('user:view', 'View Users', 'MENU', NULL, '/users', 'GET', 1),
  ('user:manage', 'Manage Users', 'API', NULL, '/api/users', 'POST', 1),
  ('role:manage', 'Manage Roles', 'API', NULL, '/api/roles', 'POST', 1),
  ('log:view', 'View Logs', 'MENU', NULL, '/logs', 'GET', 1)
ON DUPLICATE KEY UPDATE
  perm_name = VALUES(perm_name),
  perm_type = VALUES(perm_type),
  path = VALUES(path),
  method = VALUES(method),
  status = VALUES(status);

-- 初始化默认管理员
-- 默认用户名：admin
-- 默认密码哈希为预置占位值，后续在后端登录模块接入后建议立即重置
INSERT INTO sys_user (username, password_hash, nickname, status, user_type)
VALUES
  (
    'admin',
    '$2a$10$0B8mW8mQvM2wL0z7Q3hQ4eaXx1n4M8nYtMZ0p2C1wYQhKXJwJ5R0K',
    '系统管理员',
    1,
    'ADMIN'
  )
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  status = VALUES(status),
  user_type = VALUES(user_type);

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'admin'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- 给管理员授予全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
WHERE r.role_code = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 初始化系统配置
INSERT INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES
  ('app', 'app.name', 'jc-rag', 'STRING', 0, 'Application name'),
  ('app', 'deployment.mode', 'offline-ready', 'STRING', 0, 'Offline deploy strategy'),
  ('storage', 'minio.endpoint', 'http://localhost:9000', 'STRING', 0, 'MinIO endpoint'),
  ('storage', 'minio.accessKey', 'minioadmin', 'STRING', 0, 'MinIO access key'),
  ('storage', 'minio.secretKey', 'minioadmin', 'STRING', 1, 'MinIO secret key'),
  ('storage', 'minio.bucket.documents', 'jc-rag-docs', 'STRING', 0, 'Document bucket'),
  ('rag', 'embedding.model.path', 'D:\\jc-rag\\models\\embedding\\bge-large-zh-v1.5', 'STRING', 0, 'Local embedding model path'),
  ('rag', 'embedding.device', 'auto', 'STRING', 0, 'auto/cpu/cuda'),
  ('rag', 'retrieval.topK', '5', 'NUMBER', 0, 'Top K results'),
  ('rag', 'retrieval.topN', '20', 'NUMBER', 0, 'Candidate recall size'),
  ('rag', 'retrieval.similarityThreshold', '0.60', 'NUMBER', 0, 'Similarity threshold'),
  ('rag', 'chunk.size', '700', 'NUMBER', 0, 'Chunk size'),
  ('rag', 'chunk.overlap', '120', 'NUMBER', 0, 'Chunk overlap'),
  ('rag', 'history.rounds', '3', 'NUMBER', 0, 'History rounds for retrieval'),
  ('model', 'active.mode', 'offline', 'STRING', 0, 'online/offline'),
  ('model', 'offline.provider', 'ollama', 'STRING', 0, 'Offline provider'),
  ('model', 'offline.ollama.baseUrl', 'http://localhost:11434', 'STRING', 0, 'Ollama base url'),
  ('model', 'offline.ollama.chatPath', '/api/chat', 'STRING', 0, 'Ollama chat path'),
  ('model', 'offline.ollama.tagsPath', '/api/tags', 'STRING', 0, 'Ollama model list path'),
  ('model', 'offline.ollama.defaultModel', 'qwen1.5-1.8b-gguf', 'STRING', 0, 'Preferred local model'),
  ('model', 'default.temperature', '0.3', 'NUMBER', 0, 'Default temperature'),
  ('model', 'default.topP', '0.8', 'NUMBER', 0, 'Default top_p'),
  ('model', 'default.maxTokens', '1024', 'NUMBER', 0, 'Default max tokens'),
  ('service', 'python.rag.baseUrl', 'http://localhost:8001', 'STRING', 0, 'Python RAG service base url'),
  ('service', 'frontend.baseUrl', 'http://localhost:5173', 'STRING', 0, 'Frontend dev url')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  value_type = VALUES(value_type),
  encrypted = VALUES(encrypted),
  remark = VALUES(remark);

-- 初始化模型配置
INSERT INTO llm_provider_config (
  provider_type,
  provider_name,
  endpoint,
  api_key_encrypted,
  model_name,
  enabled,
  priority_no,
  extra_config_json
)
VALUES
  (
    'OFFLINE',
    'ollama-local',
    'http://localhost:11434/api/chat',
    NULL,
    'qwen1.5-1.8b-gguf',
    1,
    1,
    JSON_OBJECT(
      'baseUrl', 'http://localhost:11434',
      'chatPath', '/api/chat',
      'tagsPath', '/api/tags',
      'stream', true
    )
  ),
  (
    'ONLINE',
    'openai-compatible',
    'https://api.example.com/v1/chat/completions',
    NULL,
    'gpt-4o-mini',
    0,
    100,
    JSON_OBJECT(
      'baseUrl', 'https://api.example.com',
      'chatPath', '/v1/chat/completions',
      'stream', true
    )
  )
ON DUPLICATE KEY UPDATE
  endpoint = VALUES(endpoint),
  model_name = VALUES(model_name),
  enabled = VALUES(enabled),
  priority_no = VALUES(priority_no),
  extra_config_json = VALUES(extra_config_json);

-- 初始化 RAG 参数
INSERT INTO rag_runtime_config (
  id,
  chunk_size,
  chunk_overlap,
  top_k,
  top_n,
  similarity_threshold,
  enable_hybrid_search,
  enable_rerank,
  history_rounds,
  embedding_model_path,
  embedding_device
)
VALUES
  (
    1,
    700,
    120,
    5,
    20,
    0.6000,
    0,
    0,
    3,
    'D:\\jc-rag\\models\\embedding\\bge-large-zh-v1.5',
    'auto'
  )
ON DUPLICATE KEY UPDATE
  chunk_size = VALUES(chunk_size),
  chunk_overlap = VALUES(chunk_overlap),
  top_k = VALUES(top_k),
  top_n = VALUES(top_n),
  similarity_threshold = VALUES(similarity_threshold),
  enable_hybrid_search = VALUES(enable_hybrid_search),
  enable_rerank = VALUES(enable_rerank),
  history_rounds = VALUES(history_rounds),
  embedding_model_path = VALUES(embedding_model_path),
  embedding_device = VALUES(embedding_device);

-- 初始化基础 Skill
INSERT INTO skill_definition (
  skill_code,
  skill_name,
  skill_type,
  prompt_template,
  input_schema_json,
  output_schema_json,
  allowed_tools_json,
  enabled,
  version
)
VALUES
  (
    'doc_retrieval',
    '文档检索技能',
    'BASE',
    '你是军事知识检索助手。请优先依据召回文档内容回答，未检索到依据时明确说明。',
    JSON_OBJECT('query', 'string', 'kbId', 'number'),
    JSON_OBJECT('answer', 'string', 'citations', 'array'),
    JSON_ARRAY('rag_search', 'doc_lookup'),
    1,
    1
  ),
  (
    'regulation_interpretation',
    '条令解读技能',
    'DOMAIN',
    '你是军事条令解读助手。请基于条令原文进行结构化解读，输出关键条款、适用场景、注意事项，并附引用。',
    JSON_OBJECT('question', 'string', 'docType', 'string'),
    JSON_OBJECT('summary', 'string', 'keyPoints', 'array', 'citations', 'array'),
    JSON_ARRAY('rag_search', 'answer_generate'),
    1,
    1
  ),
  (
    'training_plan_generation',
    '训练计划制定技能',
    'DOMAIN',
    '你是军事训练计划助手。请基于训练大纲、考核标准和用户要求，生成分阶段训练计划，并说明引用依据。',
    JSON_OBJECT('goal', 'string', 'period', 'string', 'participants', 'string'),
    JSON_OBJECT('plan', 'string', 'milestones', 'array', 'citations', 'array'),
    JSON_ARRAY('rag_search', 'template_render', 'answer_generate'),
    1,
    1
  )
ON DUPLICATE KEY UPDATE
  skill_name = VALUES(skill_name),
  skill_type = VALUES(skill_type),
  prompt_template = VALUES(prompt_template),
  input_schema_json = VALUES(input_schema_json),
  output_schema_json = VALUES(output_schema_json),
  allowed_tools_json = VALUES(allowed_tools_json),
  enabled = VALUES(enabled),
  version = VALUES(version);
