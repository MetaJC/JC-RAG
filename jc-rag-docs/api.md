# JC RAG - 前端 API 与模块路由

> 前端请求统一前缀 `/api/<module>/<resource>`，与后端控制器一一对应。

## 路由前缀

| 模块 | 前缀      | 前端封装                | 备注                          |
|------|-----------|-------------------------|-------------------------------|
| rbac | /api/rbac | src/api/rbac.ts         | 用户/角色/权限/菜单（保留）   |
| sys  | /api/sys  | src/api/sys.ts          | Dashboard + RBAC + 配置/健康 |
| rag  | /api/rag  | src/api/rag.ts          | 智能体 / 技能 / 提示词        |
| auth | /api/auth | src/api/auth.ts         | 登录 / 验证码                 |
| chat | /api/chat | src/api/chat.ts         | 聊天                          |
| kb   | /api/kb   | src/api/knowledge.ts    | 知识库                        |
| audit| /api/audit| src/api/audit.ts        | 操作日志                      |

## src/api 目录结构

```
src/api/
├── http.ts          # axios 实例
├── auth.ts          # 登录/验证码
├── rbac.ts          # RBAC 模块 API
├── sys.ts           # sys 模块 API (Dashboard + RBAC + Runtime + Health + Demo + ApiKey)
├── rag.ts           # rag 模块 API (Agent + Skill + Prompt)
├── chat.ts          # chat 模块 API
├── knowledge.ts     # knowledge 模块 API
├── audit.ts         # audit 模块 API
└── admin.ts         # 临时杂项（仅保留 data-permission，后续并入 sys）
```

## sys.ts 暴露的方法

| 分组     | 方法                                                                 |
|----------|----------------------------------------------------------------------|
| Dashboard| fetchSysDashboard                                                   |
| RBAC     | fetchSysUsers / fetchSysRoles / fetchSysPermissions / fetchSysMenus  |
|          | fetchSysMenuTree / createSysMenu / updateSysMenu / deleteSysMenu     |
|          | moveSysMenu / createSysUser / updateSysUser / deleteSysUser          |
|          | resetSysUserPassword / createSysRole / updateSysRole / deleteSysRole |
|          | createSysPermission / updateSysPermission / deleteSysPermission      |
| Runtime  | fetchRuntimeConfig / saveRuntimeConfig                               |
| Health   | fetchServiceHealth                                                   |
| 扩展     | fetchOllamaModels / fetchDevices / fetchSystemResources / verifyModel / browseDirectory |
| Demo     | fetchDemoConfig / updateDemoConfig / fetchRoleQuotas / saveRoleQuota / checkQuota |
| ApiKey   | fetchUserApiKeys / fetchActiveApiKey / saveUserApiKey / toggleUserApiKey / deleteUserApiKey |

## rag.ts 暴露的方法

| 分组   | 方法                                                              |
|--------|-------------------------------------------------------------------|
| Agent  | fetchRagAgents / getRagAgent / createRagAgent / updateRagAgent    |
|        | publishRagAgent / unpublishRagAgent / deleteRagAgent             |
| Skill  | fetchRagSkills / getRagSkill / createRagSkill / updateRagSkill / deleteRagSkill |
| Prompt | fetchRagPrompts / getRagPrompt / createRagPrompt / updateRagPrompt / deleteRagPrompt |

## knowledge.ts 暴露的方法

| 分组        | 方法                                                                                                |
|-------------|-----------------------------------------------------------------------------------------------------|
| 知识库 CRUD | fetchKnowledgeBases / createKnowledgeBase / updateKnowledgeBase / deleteKnowledgeBase              |
| 默认知识库  | fetchSystemDefaultKb / setSystemDefaultKb / clearSystemDefaultKb (系统级, 仅超管/developer)        |
| 授权管理    | fetchKbAuthDetail / saveKbAuth (`/knowledge-bases/{kbId}/auth`)                                      |
| 文档管理    | fetchKnowledgeDocuments / uploadKnowledgeDocuments / reprocessKnowledgeDocument / updateKnowledgeDocument / deleteKnowledgeDocument |
| 文档文件    | getDocumentFileUrl / fetchDocumentParseProgress / cancelDocumentParse                                |
| 策略/资源   | fetchKnowledgeUploadPolicy / fetchSystemResources                                                    |

## 视图 -> API 映射

| 视图                     | 使用 API                                |
|--------------------------|------------------------------------------|
| RbacUserView 等 RBAC 视图 | src/api/rbac.ts                          |
| SystemSettingsView       | src/api/sys.ts (运行时/健康/扩展)         |
| AgentSkillManageView     | src/api/rag.ts + src/api/rbac.ts (角色)  |
| PromptManageView         | src/api/rag.ts                           |
| DemoConfigView           | src/api/sys.ts                           |
| DataPermissionView       | src/api/admin.ts (临时)                  |
| KnowledgeBaseView        | src/api/knowledge.ts (含默认 KB / 授权跳转) |
| KnowledgeBaseAuthView    | src/api/knowledge.ts (fetchKbAuthDetail / saveKbAuth) |

## 旧路径迁移

| 旧路径                                  | 新路径                              | 后端 Controller                |
|----------------------------------------|-----------------------------------|---------------------------------|
| /api/system/runtime-config             | /api/sys/runtime-config            | SysConfigController             |
| /api/system/health/services            | /api/sys/health/services           | SysHealthController             |
| /api/system/ollama/models              | /api/sys/ollama/models             | SysExtraController              |
| /api/system/devices                    | /api/sys/devices                   | SysExtraController              |
| /api/system/embedding/verify           | /api/sys/embedding/verify          | SysExtraController              |
| /api/system/browse-directory           | /api/sys/browse-directory          | SysExtraController              |
| /api/admin/prompts                     | /api/rag/prompt                    | RagPromptController             |
| /api/admin/skills                      | /api/rag/skill                     | RagSkillController              |
| /api/admin/agents                      | /api/rag/agent                     | RagAgentController              |
| /api/admin/demo-config                 | /api/sys/demo-config               | SysDemoModeController           |
| /api/admin/demo-config/role-quotas     | /api/sys/role-quotas               | SysDemoModeController           |
| /api/system/devices                    | /api/sys/devices (推荐) / /api/sys/_legacy/devices (兜底) | SysExtraController    |
| /api/system/embedding/verify           | /api/sys/embedding/verify (推荐) / /api/sys/_legacy/embedding/verify (兜底) | SysExtraController |
| /api/system/browse-directory           | /api/sys/browse-directory (推荐) / /api/sys/_legacy/browse-directory (兜底) | SysExtraController |
