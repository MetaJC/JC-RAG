# JC RAG 后端模块说明

> 路径规则: 控制器类级 `@RequestMapping = /api/<module>/<resource>`，资源名单数；与文件路径一一对应。

## 模块清单

| 模块     | 前缀        | 职责                                                                |
|----------|-------------|---------------------------------------------------------------------|
| rbac     | /api/rbac   | 用户 / 角色 / 权限 / 菜单 |
| sys      | /api/sys    | Dashboard / 日志 / 通知 / 运行时配置 / 健康 / 扩展 / Demo / API Key / 法律文档 |
| rag      | /api/rag    | 智能体 / 技能 / 提示词 |
| auth     | /api/auth   | 登录 / 验证码 / 修改密码 / 短信验证码登录注册 (SmsAuthController) |
| chat     | /api/chat   | 聊天会话 / 消息 |
| knowledge| /api/kb     | 知识库 / 文档 |
| audit    | /api/audit  | 操作日志查询 (兼容层, 统一入口为 /api/sys/log/*) |
| health   | /api/health | 基础健康检查 (Security 白名单)                                      |

## sys 模块

### Controller

| 类名                    | 路径前缀                          | 说明                       |
|-------------------------|-----------------------------------|----------------------------|
| DashboardController     | /api/sys/dashboard                | 工作台数据                 |
| SysConfigController     | /api/sys/runtime-config           | 运行时配置                 |
| SysHealthController     | /api/sys/health/services          | 服务健康 (Spring/Python/ES) |
| SysHealthBasicController| /api/health/basic                 | 基础健康 (白名单)          |
| SysExtraController      | /api/sys/{ollama/models,devices,resources,embedding/verify,browse-directory} + /api/sys/_legacy/{devices,embedding/verify,browse-directory} | Ollama/设备/资源/模型验证/目录；_legacy 兼容旧 /api/system/** |
| SysApiKeyController     | /api/sys/api-keys                 | C 端用户 API Key 管理      |
| SysLegalDocController   | /api/sys/legal-doc                | 用户协议 / 隐私政策 (A 端管理) |
| DictController          | /api/sys/dict                     | 字典                       |
| ModelProviderConfigController | /api/model/providers        | 模型服务商配置 (含 L3 账户级 /v1/models 刷新) |
| ModelCatalogController  | /api/model/catalog                | 模型目录 (L1 内置 + L2 远程同步, 含类型与能力标注) |
| SysNotifyController     | /api/sys/notify                   | 站内通知 (原 NotificationController) |
| SysLogController        | /api/sys/log/{operation,exception,rag-call} | 操作日志 / 异常日志 / RAG 调用日志 (统一入口) |
| AuditLogQueryController | /api/audit/{rag-call-logs,exception-logs} | [兼容层 @Deprecated] 旧路径, 新接口用 SysLogController |

### Service

`RuntimeConfigService` / `SystemHealthService` / `SysDemoModeService` / `SysApiKeyService` / `DashboardService` / `DictService` / `NotificationService` / `OperationLogService` / `ModelProviderConfigService` / `ModelCatalogService`，均遵循 `Interface + Impl` 模式。

### 模型目录三层同步机制

| 层 | 机制 | 数据源 |
|----|------|--------|
| L1 内置目录 | 随版本发布, 含模型类型标注 (chat/embedding/rerank/image/video/audio/other) 与能力数据 (inputs/outputs 模态 + features 特性) | `jc-rag-admin/src/main/resources/model-catalog.json` |
| L2 远程目录 | 可配置 URL + TTL 缓存 (sys_config `model.catalog.cache`), 失败自动回退 | sys_config `model.catalog.remoteUrl` / `model.catalog.ttlHours` (默认 168h) |
| L3 账户实时 | 已配置的 API Key 调服务商 /v1/models | `ModelProviderConfigService#refreshModels` |

目录模型条目结构：`name` / `type` / `inputs`(text/image/video/audio) / `outputs` / `features`(tool-call/thinking/web-search/structured-output/coding/cache)。前端能力标签优先使用目录维护的能力数据，仅 L3 实时结果与旧内置列表回退到名称启发式推断；能力筛选标签支持多选。

读取优先级: TTL 内远程缓存 &gt; 远程拉取 &gt; 过期缓存 &gt; 内置目录。Embedding 来源 (local/service) 持久化在 sys_config `rag.embedding.source` 与 `rag.embedding.service.*`, Java 端通过 `buildRagOverrideForPython()` 下发给 Python, 在线模式走 OpenAI 兼容 `/embeddings` 接口。

### 模型仪表盘生效模型展示规则

`ModelDashboardView` 按类别每行展示当前生效模型 (社区版固定两行: 对话模型 / 嵌入模型), 服务商与模型名称拆分为独立字段。每类模型仅有一个生效配置, 三选一互斥: 在线模型服务 (按 `model.online.baseUrl` 反查 `llm_provider_config` 取服务商展示名) / 本地模型服务 (对话固定 Ollama) / 本地下载模型代码直调 (当前仅嵌入模型支持, 服务商列显示"本地模型")。图像生成、视频生成等多模态模型类别为商业版/企业版功能, 社区版不展示。

## rag 模块

### Controller

| 类名                  | 路径前缀                | 说明                         |
|-----------------------|-------------------------|------------------------------|
| RagAgentController    | /api/rag/agent          | 智能体管理（CRUD + publish） |
| RagSkillController    | /api/rag/skill          | 技能管理                     |
| RagPromptController   | /api/rag/prompt         | 提示词管理（global/role/agent 作用域） |

### Service

`RagAgentService` / `RagSkillService` / `RagPromptService` + Impl。列表分页统一走 `PageRequest + PageResponse`，SQL 写在 XML。

### 表

| 表名                | 实体                          | 说明                |
|---------------------|-------------------------------|---------------------|
| sys_agents          | RagAgentEntity                | 智能体              |
| sys_skills          | RagSkillEntity                | 技能                |
| sys_agent_skills    | RagAgentSkillEntity           | 智能体-技能关联     |
| sys_agent_roles     | RagAgentRoleEntity            | 智能体-角色关联     |
| sys_prompts         | RagPromptEntity               | 提示词              |
| rag_call_log        | RagCallLog                    | RAG 调用日志        |

## sys 模块表 (数据字典)

| 表名                | 实体                          | 说明                |
|---------------------|-------------------------------|---------------------|
| sys_operation_log   | SysOperationLog               | 操作日志 (AOP 自动记录) |
| sys_exception_log   | SysExceptionLog               | 系统异常日志 (GlobalExceptionHandler) |
| sys_config          | SysConfigEntity               | 运行时配置键值      |
| sys_role            | RbacRoleEntity                | 角色 (rbac_ 命名空间) |
| sys_user            | RbacUserEntity                | 用户 (rbac_ 命名空间) |
| sys_user_role       | RbacUserRoleEntity            | 用户-角色关联 (rbac_) |
| sys_role_permission | RbacRolePermissionEntity      | 角色-权限关联 (rbac_) |
| sys_permission      | RbacPermissionEntity          | 权限 (rbac_) |

> 命名空间说明: 用户/角色/权限/菜单用 `rbac_` 前缀; 系统级日志/配置用 `sys_` 前缀; 智能体/技能/提示词保留 `sys_` 前缀 (历史命名, 暂未迁移)。

### rbac_role 关键字段

| 字段       | 类型        | 默认 | 说明 |
|------------|-------------|------|------|
| role_code  | VARCHAR(64) | 必填 | 业务编码 (不可重复), `super_admin`/`admin`/`analyst`/`viewer` 等 |
| role_name  | VARCHAR(64) | 必填 | 展示名 |
| status     | TINYINT     | 1    | 启用状态 (1 启用 / 0 禁用) |
| **is_admin** | TINYINT   | 0    | **是否允许登录管理端 (0 否 / 1 是)**；普通角色默认禁止；`super_admin` 强制 1 且业务层校验不可改为 0 (`developer` 账号除外) |

登录管理端准入校验: `AuthServiceImpl.login` 调用 `RbacRoleMapper.countAdminRoleByUserId`，用户至少拥有一个 `is_admin=1` 且 `status=1` 的角色才允许登录；`developer` 兜底账号绕过此检查 (SQL: `021_rbac_role_is_admin.sql`)。

## knowledge 模块

### Controller

| 类名                          | 路径前缀                                | 说明                                |
|-------------------------------|-----------------------------------------|-------------------------------------|
| KnowledgeBaseController       | /api/knowledge-bases                    | 知识库 / 文档 CRUD + 上传 + 解析进度 |
| KbAuthController              | /api/knowledge-bases/{kbId}/auth        | 知识库授权管理 (角色 + 用户)         |
| SystemKbDefaultController     | /api/admin/system/kb-default            | 系统级 C 端默认知识库 (超管/developer) |

### Service

`KnowledgeBaseService` / `KbPermissionService` / `KbAuthService` + Impl，遵循 `Interface + Impl` 模式，SQL 写在 XML (kb_role_auth / kb_user_auth Mapper)。

### 权限模型

#### 可见范围 (visibility_scope)

| 枚举        | 数据库值  | 说明                                                                |
|-------------|-----------|---------------------------------------------------------------------|
| PRIVATE     | PRIVATE   | 仅所有者可见/操作                                                    |
| INTERNAL    | INTERNAL  | 全 ADMIN 可见；编辑/管理需单独授权 (历史值 `ROLE` 自动迁移到 `INTERNAL`) |
| PUBLIC      | PUBLIC    | 全 ADMIN 可见/编辑；管理需单独授权；可设置为 C 端默认/允许客户端选择 |

#### 操作权限 (perm_level) — 高级别覆盖低级别

| 枚举    | 值 | 包含   | 描述                                  | 前端展示 |
|---------|----|--------|---------------------------------------|----------|
| NONE    | 0  | -      | 无权限                                | 未授权   |
| VIEW    | 1  | -      | 查看 / 下载文档                       | 可查看   |
| EDIT    | 2  | VIEW   | 上传 / 删除 / 重命名 / 重建索引        | 可编辑   |
| MANAGE  | 3  | EDIT   | 删除 KB / 可见范围变更 / 授权他人      | 可管理   |

#### 授权表

| 表名            | 实体                  | 说明                                                       |
|-----------------|-----------------------|------------------------------------------------------------|
| kb_role_auth    | KbRoleAuthEntity      | 知识库-角色授权 (`role_id` 取 `rbac_role.id`)              |
| kb_user_auth    | KbUserAuthEntity      | 知识库-用户授权, 限定 `user_type=ADMIN` (开发中过滤 NORMAL) |

#### C 端知识库选择 (UserChatService.getKbSelectConfig)

模式互斥, 二选一:

1. **FIXED**: 系统配置 `chat.default.kbId` 命中, C 端不显示下拉, 固定使用该 KB (仅超管/developer 可设置, KB 必须为 PUBLIC)
2. **SELECTABLE**: 公开且 `client_selectable=1` 的 KB 列表, C 端用户自行选择
3. **EMPTY**: 无任何可用 KB, C 端拒绝创建会话

> 设置非 PUBLIC KB 为默认时, 前端弹出确认: 升级为 PUBLIC 并设为默认, 沿用原有的「所有者保留」语义。

### 表

| 表名                 | 实体                          | 说明                  |
|----------------------|-------------------------------|-----------------------|
| kb_knowledge_base    | KbKnowledgeBaseEntity         | 知识库 (含 client_selectable) |
| kb_document          | KbDocumentEntity              | 文档                  |
| kb_document_version  | KbDocumentVersionEntity       | 文档版本              |
| kb_document_chunk_task | KbDocumentChunkTaskEntity    | 分块任务              |
| kb_role_auth         | KbRoleAuthEntity              | 知识库-角色授权        |
| kb_user_auth         | KbUserAuthEntity              | 知识库-用户授权        |

## chat 模块 (C 端) 与记忆子系统

### 记忆子系统 (jc-rag-user + jc-rag-service, 2026-07-03 全量上线)

> 设计文档: `architecture/memory-system.md` (L0 / L1 / L2 / L3 + GDPR + retention)
> 完整功能: L0 短期滑动窗口 + L1 摘要压缩 + L2 长期事实 (ES 向量) + L3 跨会话画像 + P0-1 一键忘记我 + P0-2 矛盾事实 retract + P0-3 L0 retention 后台清理

#### Controller (Java)

| 类名 | 路径前缀 | 说明 |
|------|----------|------|
| UserChatController | /api/chat | C 端聊天会话 / 消息 (含 L0 history / L1 summary / L2 recall / L3 profile 触发) |
| UserPreferenceController | /api/user/preference | 偏好 (5 项 OK + 被遗忘权) + 记忆文件 CRUD + forget-me |
| UserMemoryController | /api/user/preference/memory | L2 长期事实列表/软删/清空 + L3 画像查询/重置 |

#### Service (Java)

| 接口 | 职责 |
|------|------|
| UserChatService | 流式问答编排, 串联 L0/L1/L2/L3, 触发 L1 摘要/L2 提取/L3 画像异步任务 |
| UserPreferenceService | 偏好读写 + 记忆文件 IO (含 `~` 展开 + 原子写) + `forgetMe` 8 步物理删除 |
| UserMemoryService | L2/L3 数据编排: 调 Python `/chat/memory/*` / `/chat/profile/*`, 维护 `user_memory_profile` 表 |
| ChatHistoryRetentionService | 后台 `@Scheduled(cron="0 0 3 * * ?")` 物理删 N 天前 L0/L1 数据 (默认 90) |

#### Service (Python, jc-rag-service/app/api/routers)

| 路由文件 | 端点 | 职责 |
|----------|------|------|
| chat.py | `/chat/ask/stream` | 注入 L0 history + L1 summary + L2 facts, 流式生成 |
| chat.py | `/chat/summary` | L1 摘要生成 (Java 异步触发) |
| memory.py | `/chat/memory/extract` | L2 提取 (含 `retract` 矛盾事实处理) |
| memory.py | `/chat/memory/search` | L2 向量检索 (user_id 过滤, topK=5, threshold=0.6) |
| memory.py | `/chat/memory/list` / `clear` / `delete` | 前端 "我的记忆" 页 CRUD |
| memory.py | `/chat/memory/forget` | P0-1: ES `delete_by_query` 物理删 |
| memory.py | `/chat/memory/retract` | P0-2: 单条 fact 物理删 |
| profile.py | `/chat/profile/extract` | L3 画像提取 (五段结构) |

#### Python 工具 (jc-rag-service/app/utils)

| 文件 | 职责 |
|------|------|
| `memory_store.py` | ES `jc_chat_memory` 索引 CRUD (含 `clear_facts(hard=True)` / `delete_fact_by_value`) |

#### MySQL 表 (记忆子系统)

| 表名 | 实体 | 说明 |
|------|------|------|
| chat_session | ChatSessionEntity | L0 会话 |
| chat_message | ChatMessageEntity | L0 消息 (有 `idx_chat_message_created` 索引供 retention) |
| chat_session_summary | ChatSessionSummaryEntity | L1 摘要 (含 status 字段, 新摘要触发时旧摘要失效) |
| user_memory_profile | UserMemoryProfileEntity | L3 画像 (五段 JSON, 详见 SQL 023) |

#### ES 索引 (记忆子系统)

| 索引名 | mapping | 用途 |
|--------|---------|------|
| jc_chat_memory | user_id / key / value / content_vector(384) / active / created_at | L2 长期事实 (bge-large-zh-v1.5 向量) |

#### 端点总览 (C 端用户接口, 全部在 jc-rag-user:8082)

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | /api/user/preference | 加载本用户全部偏好 (含默认值填充) |
| PUT | /api/user/preference | 更新偏好 (按字段非空更新) |
| GET | /api/user/preference/memory-file | 读记忆文件 (Markdown, 不存在则自动初始化) |
| PUT | /api/user/preference/memory-file | 保存记忆文件 (原子写) |
| POST | /api/user/preference/forget-me | **P0-1** 一键忘记我 (8 步物理删除) |
| GET | /api/user/preference/memory/list | L2 长期事实分页列表 |
| POST | /api/user/preference/memory/delete | L2 软删单条 |
| POST | /api/user/preference/memory/clear | L2 清空 (软删, 留审计) |
| GET | /api/user/preference/memory/profile | L3 画像查询 |
| DELETE | /api/user/preference/memory/profile | L3 画像重置 |

#### 配置项 (application-dev.yml + rag_config.yml)

| Key | 默认 | 说明 |
|-----|------|------|
| `jc-rag.memory.l0.retention-days` | 90 | L0/L1 物理保留天数 (0/负数=关闭) |
| `jc-rag.memory.l0.retention-cron` | `0 0 3 * * ?` | L0 retention 调度时间 |
| `jc-rag.memory.profile.trigger-rounds` | 10 | L3 画像触发轮数 |
| `rag.memory.l2.max-per-user` | 500 | L2 单用户最大 fact 数 |
| `rag.memory.l2.similarity-threshold` | 0.6 | L2 检索阈值 |
| `rag.memory.l2.dedup-threshold` | 0.9 | L2 相似去重阈值 |
| `rag.memory.l1.summary-interval-rounds` | 20 | L1 摘要触发轮数 |

## 旧接口兼容

`system` 模块下已不保留任何 Controller，原 `/api/system/**` 通过 `SysExtraController#_legacy/**` 和 `SysConfigController#runtime-config` 等同位置 `/api/sys/**` 路由接入。建议前端在合理窗口内切换到 `/api/sys/**`。

## 短信服务模块 (jc-rag-sms)

独立 Maven 模块 (`jc-rag-backend/jc-rag-sms`), 提供短信渠道抽象 (log/aliyun/tencent 可配置切换) + 验证码发送/限流/校验, 支撑 C 端手机号验证码登录/注册一体 (`POST /api/auth/sms/{send,login}`, 位于 jc-rag-user)。详细设计见 [`architecture/sms-service.md`](sms-service.md)。
