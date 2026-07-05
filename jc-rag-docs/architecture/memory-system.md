# JC RAG 记忆系统设计 (Memory System)

> 本文档记录 JC RAG 的"对话记忆"完整设计, 当前阶段已落地的: **L0 短期记忆 (✅)**、**L1 摘要压缩 (✅)**、**L2 长期事实 (✅)**、**L3 跨会话画像 (✅)**、**偏好设置入口 + 记忆文件位置 + 我的记忆页 (✅)**、**一键忘记我 GDPR (✅)**、**L2 矛盾事实 retract (✅)**、**L0 retention 后台清理 (✅)**。
> 商业差异化 (专业版/团队版) 作为 **待办**, 不在本期实现。

---

## 一、设计目标

| 维度 | 目标 |
|------|------|
| 至少支撑 | 100 轮对话上下文无损 |
| 对标 | ChatGPT Memory + Mem0 事实检索 + Trae/Cursor 项目记忆 |
| 可观测 | 前端展示"AI 想起了什么" |
| 可控 | 用户可清空记忆 / 关闭长期记忆 / 改记忆文件位置 / 一键忘记我 |
| Token 经济 | 100 轮不爆 token, 每轮喂 ~3000 tokens |
| 多端一致 | 用户端/管理端/未来小程序共享同一份记忆 |
| 合规 | 支持 GDPR / 个保法 (一键物理删除本用户全量数据, 不可逆) |
| 数据生命周期 | L0/L1 默认 90 天 retention, L2/L3 用户主动管理; 配 0 即关 |
| 开源边界 | 核心记忆能力 100% 开源; 商业版独占团队/组织级能力 |

---

## 二、分层架构 (L0 ~ L3)

### 2.1 总览

```
                  ┌────────────────────────────────────┐
                  │  用户问 "分库分表怎么选?"           │
                  └─────────────────┬──────────────────┘
                                    ▼
            ┌──────────────────────────────────────────────┐
            │  Java UserChatServiceImpl                     │
            │  ──────────────────────────────────────────  │
            │  1. 读最近 20 条 (L0 短期)                    │
            │  2. 读本会话 summary (L1 摘要)                │
            │  3. 检索 L2 事实 (ES jc_chat_memory)          │
            │  4. 拼装 messages 数组 → 调 Python            │
            └─────────────────┬────────────────────────────┘
                              │  {history, memoryFacts}
                              ▼
            ┌──────────────────────────────────────────────┐
            │  Python rag-service                          │
            │  ──────────────────────────────────────────  │
            │  1. 接收 history + memoryFacts               │
            │  2. 向量化当前 question                      │
            │  3. ES 检索相关 chunk (RAG)                  │
            │  4. 拼 prompt = RAG + history                │
            │  5. 调 LLM 流式生成                          │
            └──────────────────────────────────────────────┘
                              │  异步 (不影响本次回复)
                              ▼
            ┌──────────────────────────────────────────────┐
            │  异步任务: 记忆提取 (Java @Async)             │
            │  ──────────────────────────────────────────  │
            │  1. 把新对话发给 LLM 提取 fact               │
            │  2. 向量化 fact → ES jc_chat_memory          │
            │  3. 检测是否需要 L1 摘要压缩                  │
            └──────────────────────────────────────────────┘
```

### 2.2 L0 短期记忆 (滑动窗口) — **✅ 已完成 (2026-07-03)**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 数据源 | MySQL `chat_message` (已有) | 同 |
| 范围 | 当前会话最近 K 轮 | 同 |
| K | 默认 10, 按 token 预算动态截断 (默认 4000 tokens) | 默认 10 轮, 按 `HISTORY_MAX_CHARS=8000` 字符截断 |
| 取法 | `message_index desc limit 20`, 倒序组装 | MyBatis-Plus `orderByDesc("message_index") LIMIT 20` |
| 注入 | `messages: [system, ...history, user(question)]` | Java 拼 `history` 字段 → Python `/chat/ask/stream` 透传到 LLM `messages` 数组 |

**关键 API 变更**：
- Java `UserChatServiceImpl.buildShortTermHistory()` — 读 chat_message 拼 history, 截断到 8000 字符
- Python `ChatAskRequest.history: list[HistoryMessage]` — 已加, 在 `chat_ask_stream` / `chat_ask` 中拼入 messages
- 前端零改动: 用户体验不变, "上一句问上海天气, 下一句问明天呢" AI 能正确关联

### 2.3 L1 摘要压缩 (滚动摘要) — **✅ 已完成 (2026-07-03)**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 触发 | 同一会话超过 20 轮时异步压缩前 10 轮 | `maybeTriggerSummary` 在 20/40/60... 轮触发, 取前 10 轮 USER/ASSISTANT |
| 存储 | MySQL `chat_session_summary` (待建表) | **已建表**, 字段: id / session_id / user_id / summary_text / covered_from_index / covered_to_index / token_count / status / model_name / created_at / updated_at |
| 注入 | 同 L0, 放在 system prompt 之后, L0 history 之前 | `loadLatestSummaryText` 取本会话最新有效摘要, 拼到 `systemPrompt` 头部 (带 `[本会话历史摘要 (L1)]` 前缀) |
| 异步 | LLM 摘要在后台跑, 不阻塞主对话 | `STREAM_EXECUTOR.execute(() -> generateSummary(...))` 异步执行 |
| 降级 | LLM 失败兜底 | 3 次重试均失败 → `buildSummaryFromFallback` 取前 5 轮 (10 条) 截断到 600 字直接拼接 |
| 失效 | 同会话再次压缩, 旧摘要失效 | `summaryMapper.invalidateAllActive` 把 status=1 全置 0, 再插入新摘要 (status=1) |

**建表 SQL** (已执行):
```sql
CREATE TABLE chat_session_summary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  summary_text TEXT NOT NULL COMMENT '摘要正文 (脱敏后)',
  covered_from_index INT NOT NULL COMMENT '覆盖 message_index 起始 (含)',
  covered_to_index INT NOT NULL COMMENT '覆盖 message_index 结束 (含)',
  token_count INT DEFAULT NULL COMMENT '摘要 token 估算',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=有效 0=失效',
  model_name VARCHAR(64) DEFAULT NULL COMMENT '生成摘要使用的模型名',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_summary_session (session_id, status)
) COMMENT='会话滚动摘要表 (L1 摘要压缩)';
```

**关键文件**:
- `jc-rag-domain/.../entity/chat/ChatSessionSummaryEntity.java` — 实体
- `jc-rag-domain/.../mapper/chat/ChatSessionSummaryMapper.java` + `ChatSessionSummaryMapper.xml` — Mapper / SQL
- `jc-rag-user/.../service/UserChatServiceImpl.java` — `loadLatestSummaryText` / `maybeTriggerSummary` / `generateSummary` / `callPythonGenerateSummary` / `buildSummaryFromFallback` / `saveSummary`
- `jc-rag-service/app/api/routers/chat.py` — 新增 `POST /chat/summary` 端点

**端到端测试 (建议)**:
1. 重启 jc-rag-user + jc-rag-service
2. 进入任意会话, 连续发 20 轮 (1 轮 = 1 user + 1 assistant = 2 条消息)
3. 第 20 轮回答后, 看后端日志: `L1 summary done: sessionId=xxx, coveredTo=20, summaryLen=xxx, costMs=xxx`
4. 查 `chat_session_summary` 表: 应有 1 条 status=1 记录, summary_text 是压缩后的中文摘要
5. 第 21 轮开始, 提问"刚才讨论了啥"或类似引用性问题, 应能看到 AI 引用摘要内容
6. 故意把 LLM 关掉, 重发 20 轮, 应触发降级, 摘要变成"用户: ... AI: ..."的拼接文本

### 2.4 L2 长期事实 (向量记忆) — **✅ 已完成 (2026-07-03), 对标 ChatGPT Memory**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 提取 | 每次新消息后异步 LLM 提取 fact | `UserChatServiceImpl.extractMemoryAsync` 在 stream 结束后异步调用 Python `POST /chat/memory/extract`, 用 LLM 提取 fact |
| 存储 | ES `jc_chat_memory` (索引已建占位, 待启用) | **ES 索引 `jc_chat_memory` 已启用**, 字段: `memory_id / user_id / session_id / key / value / category / embedding / source / active / created_at / updated_at` |
| 检索 | 向量相似度 (cosine), user_id 过滤, topK=5, threshold=0.6 | `POST /chat/memory/recall`, user_id 过滤, topK=5, score >= 0.6 |
| 去重 | 新 fact 相似度>0.9 → update 而非 add | `memory_store.upsert_facts` 取新 fact 与该 user 已有 fact 算相似度, > 0.95 视为重复跳过, 否则新增 |
| 隔离 | user_id 隔离, **跨会话**, **不设 TTL** | 检索/写入都强制带 user_id 过滤, ES 软删 (active=false) 而非物理删除 |
| 注入 | 拼到 system prompt 头部, L0 history 之前 | Java `recallLongMemory` 同步召回, 拼为 `[用户长期记忆 (L2)]` 文本, 注入 systemPrompt |
| 异步 | 提取不阻塞主对话 | Java `@Async` (`STREAM_EXECUTOR.execute`), 失败仅 `log.warn`, 不影响用户 |
| 降级 | LLM/Embedding 失败兜底 | 提取失败 → status=FAILED, 不入库; 召回失败 → 跳过 L2 注入, 不影响 L0+L1+RAG |
| 用户可控 | 用户可关长期记忆 | 偏好设置 `enableLongMemory` 开关, 关闭后不提取也不召回 |
| 可观测 | 前端展示"我的记忆" | 独立页面 `MyMemoryView.vue`, 支持查看/搜索/筛选/删除/清空 |

**事实 Schema** (ES doc)：
```json
{
  "memory_id": "uuid",
  "user_id": 1001,
  "session_id": 12345,
  "key": "tech_stack",
  "value": "Spring Cloud Alibaba + Nacos + MyBatisPlus",
  "category": "tech_stack|career|project|preference|goal",
  "embedding": [0.12, 0.34, ...],
  "source": "auto|manual",
  "active": true,
  "created_at": "2026-...",
  "updated_at": "2026-..."
}
```

**提取 Prompt**：
```
从以下对话中提取关于用户的"长期关键事实"。
提取标准 (必须同时满足):
  1. 会影响未来对话
  2. 用户明确表达 (不是猜测)
  3. 不易随时间失效
优先: 技术栈/职业/项目背景/长期偏好/明确目标
不提取: 临时情绪/一次性事件/纯闲聊
格式: [{"key":"...","value":"...","category":"..."}]
最多 5 条; 已有事实 (去重用): {existing_facts}
对话: {messages}
```

**关键文件**:

| 文件 | 类型 | 说明 |
|------|------|------|
| `jc-rag-service/app/api/routers/memory.py` | Python 路由 | 5 个端点: `extract` / `recall` / `list` / `delete` / `clear` |
| `jc-rag-service/app/utils/memory_store.py` | Python 工具 | ES 索引初始化、CRUD、向量检索、embedding 调用 |
| `jc-rag-service/app/core/config.py` | Python 配置 | `MemoryConfig`: enabled / index_name / top_k / threshold / extract_max_count |
| `jc-rag-service/rag_config.yml` | Python 配置 | `memory.*` 配置项 |
| `UserChatServiceImpl.java` | Java Service | `recallLongMemory` / `maybeExtractMemory` / `extractMemoryAsync` / `buildProviderOverride` |
| `UserPreferenceVO.java` / `Param.java` | Java DTO | 新增 `enableLongMemory` 字段 (Boolean, 默认 true) |
| `UserPreferenceServiceImpl.java` | Java Service | 读写 `enableLongMemory` 到 `sys_config` (group=user_pref) |
| `UserPreferenceController.java` | Java Controller | 记忆管理端点 (list/delete/clear) |
| `api/memory.ts` | 前端 API | `fetchMemoryList` / `deleteMemory` / `clearMemory` |
| `MyMemoryView.vue` | 前端页面 | 长期记忆管理页 (查看/搜索/筛选/删除/清空) |
| `PreferencesDialog.vue` | 前端组件 | 长期记忆开关 + "查看/管理我的记忆" 按钮 |
| `router/index.ts` | 前端路由 | `/my-memory` 路由 |

**端到端测试 (建议)**:
1. 重启 jc-rag-user + jc-rag-service
2. 偏好设置 → "长期记忆" → 确认开关为开启状态
3. 连续多轮对话, 主动透露技术栈/职业/项目背景 (如 "我是 Java 后端, 用 Spring Cloud Alibaba")
4. 看后端日志: `L2 memory extract done: userId=xxx, facts=2, costMs=xxx`
5. 查 ES `jc_chat_memory` 索引: 应有该 user 的 fact 文档, 含 embedding 向量
6. 新建一个会话, 提问 "我用什么技术栈?" → AI 应自动回答出之前提取的事实
7. 访问 `/my-memory` → 看到事实列表, 可搜索/删除/清空
8. 关闭"长期记忆"开关 → 重新对话 → 验证不再提取, AI 也不召回
9. 删除/清空记忆 → 验证 ES 中 fact 文档 active=false 或被清空, AI 不再引用

### 2.5 L3 跨会话画像 (聚合画像) — **✅ 已完成 (2026-07-03), 对标 Mem0 add() + ChatGPT Memory**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 数据源 | L2 长期事实 (ES `jc_chat_memory`) | 同, 调 `memory_store.list_facts(user_id, page=1, size=200)` 拉全量 |
| 触发 | 每累计 N 轮对话 | `rounds % trigger-rounds == 0` 触发, **值由 `jc-rag.memory.profile.trigger-rounds` 注入 (默认 10)** |
| 提取 | LLM 把 L2 fact 聚合并提炼为五段结构化画像 | `POST /chat/profile/extract`, prompt 强约束五段 (domain/role/skills/preferences/goals), JSON 抽取+规整+容错 |
| 存储 | MySQL `user_memory_profile` (待建表) | **已建表 `023_user_memory_profile.sql`**, 字段: `id / user_id (UNIQUE) / profile_json / version / fact_count / model_name / last_extracted_at / created_at / updated_at` |
| 注入 | 拼到 systemPrompt 头部, L2 召回之前 | Java `UserMemoryProfileServiceImpl.buildPromptText` 读表, 拼为 `[用户画像 (L3) — 跨会话聚合, AI 应主动参考]` 文本, 在 `UserChatServiceImpl.buildPayload` 注入到 systemPrompt 最顶部 |
| 异步 | 提取不阻塞主对话 | Java `EXTRACT_EXECUTOR.execute(...)` 异步执行, 失败仅 `log.warn`, 不影响主对话 |
| 降级 | LLM/JSON 抽取失败兜底 | JSON 抽取失败 → 返回 `profile=null` + `message=LLM 返回非 JSON, 跳过本轮 (旧画像保留)`, **不覆盖旧画像** |
| 失效 | 同用户再次提取 | UPSERT, `version + 1`, `last_extracted_at = now()`, 旧画像被整行替换 (不是 append) |
| 用户可控 | 用户可关画像 | 偏好设置 `enableMemoryProfile` 开关, 关闭后不提取也不注入; 已有画像不会被自动删除 |
| 可观测 | 前端展示"我的画像" | 独立页面 `MyMemoryView.vue` 顶部大卡片, 5 段彩色徽章 + 渐变图标; "重置画像" 按钮调 `DELETE /memory/profile` |

**画像 Schema** (MySQL `user_memory_profile.profile_json`):
```json
{
  "domain":       "Java 后端开发",
  "role":         "高级工程师",
  "skills":       ["Spring Cloud Alibaba", "MyBatisPlus", "MySQL", "Elasticsearch"],
  "preferences":  {"code_style": "阿里规约", "answer_length": "详细", "answer_language": "中文"},
  "goals":        ["3 个月内完成 RAG 平台 v1.0 上线", "年底晋升 P7"]
}
```

**关键文件**:

| 文件 | 类型 | 说明 |
|------|------|------|
| `sql/mysql/023_user_memory_profile.sql` | SQL 脚本 | 建表 + 唯一键 (user_id) + 索引 (last_extracted_at) |
| `jc-rag-domain/.../entity/chat/UserMemoryProfileEntity.java` | Java 实体 | 8 字段 + MyBatis-Plus 注解 |
| `jc-rag-domain/.../mapper/chat/UserMemoryProfileMapper.java` | Java Mapper | `selectByUserId` 唯一查询 |
| `jc-rag-user/.../service/impl/UserMemoryProfileServiceImpl.java` | Java Service | `getProfile` / `buildPromptText` / `isEnabled` / `extractProfileAsync` / `saveProfile`(UPSERT) / `resetProfile` |
| `jc-rag-user/.../controller/UserPreferenceController.java` | Java Controller | `GET/DELETE /api/user/preference/memory/profile` |
| `UserChatServiceImpl.java` | Java Service | `buildPayload` 注入画像到 systemPrompt 头部; `maybeExtractProfile` 按 `profileTriggerRounds` 触发 |
| `UserPreferenceVO.java` / `Param.java` | Java DTO | `enableMemoryProfile` 字段 (Boolean, 默认 true) |
| `UserPreferenceServiceImpl.java` | Java Service | 读写 `enableMemoryProfile` 到 `sys_config` (group=user_pref) |
| `application-dev.yml` | Java 配置 | `jc-rag.memory.profile.trigger-rounds: 10` |
| `jc-rag-service/app/api/routers/profile.py` | Python 路由 | `POST /chat/profile/extract`, JSON 抽取+规整, 调 LLM 流式生成 |
| `jc-rag-service/app/core/config.py` | Python 配置 | `ProfileConfig` (enabled/max_skills/max_goals) + `MemoryConfig.profile` 字段 |
| `rag_config.yml` | Python 配置 | `memory.profile.{enabled, max_skills, max_goals}` |
| `api/memory.ts` | 前端 API | `fetchMyProfile` / `resetMyProfile` + `MemoryProfile` / `MemoryProfileResult` 类型 |
| `MyMemoryView.vue` | 前端页面 | 顶部 L3 画像大卡 + 5 段彩色徽章, 下方 L2 长期事实列表 |
| `PreferencesDialog.vue` | 前端组件 | L3 开关 + "查看/重置我的画像" 跳转 |
| `router/index.ts` | 前端路由 | `/my-memory` 路由 |

**端到端测试 (建议)**:
1. 重启 jc-rag-user + jc-rag-service
2. 偏好设置 → "用户画像 (L3)" → 确认开关为开启状态
3. 连续 10+ 轮对话, 主动透露技术栈/职业/项目背景/长期偏好/明确目标
4. 第 10 轮回答后, 看后端日志: `L3 profile extracted: userId=xxx, factCount=xx, costMs=xxx`
5. 查 `user_memory_profile` 表: 1 行, profile_json 是五段 JSON, version=1
6. 新建会话, 提问 "我从事什么领域? 用了什么技术栈?" → AI 应直接回答画像里的内容
7. 访问 `/my-memory` → 顶部 L3 画像大卡显示 5 段 (渐变色徽章 + 图标)
8. 点 "重置画像" → 二次确认 → 调 API → 大卡变 "暂无画像"
9. 关闭 L3 开关 → 再对话 10 轮 → 日志无 `L3 profile extracted`, AI 也不引用画像
10. 关闭 L2 + 开 L3 → L3 提取 `status=SKIPPED` (无 fact 可聚合)

**L3 与 L2 的关系**:
- L3 是 L2 的"压缩视图": L2 是 fact 列表 (零散), L3 是结构化画像 (聚合)
- L2 不可关才能用 L3 (L3 没有 fact 就聚合不出画像)
- L3 关闭不影响 L2; L2 关闭 L3 自动失效

### 2.6 一键忘记我 (GDPR / 个保法 被遗忘权) — **✅ 已完成 (2026-07-03)**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 触发 | 用户在偏好设置主动操作 | `PreferencesDialog.vue` 左侧导航 `被遗忘权 (GDPR)` 项 + 表单红色危险按钮 |
| 二次确认 | 输入 `FORGET` 三个大写字母 + 弹窗再次确认 | 前端 `ElMessageBox.prompt(inputValidator='FORGET')` → `ElMessageBox.confirm(type='error')` |
| 范围 | 物理删除本用户全部对话数据 | L0 消息 + L1 摘要 + L2 ES 长期事实 + L3 画像 + 偏好 sys_config + 记忆文件 .md; 账号本身保留 |
| 顺序 | 多步独立, 失败不抛 | `UserPreferenceServiceImpl.forgetMe` 8 步事务包裹, 每步 try/catch 仅记日志, 不阻塞 |
| 跨服务 | Java 调 Python 物理删 ES | Java `HttpClient POST /chat/memory/forget` → Python `memory_store.clear_facts(hard=True)` → ES `delete_by_query` |
| 响应 | 7 项删除条数 + 总计 | `ForgetMeResult`: sessionsFound / chatMessageDeleted / chatSessionDeleted / chatSessionSummaryDeleted / userMemoryProfileDeleted / esMemoryDeleted / userPrefDeleted / memoryFileDeleted / totalDeleted |
| 不可逆 | 是 | 不提供 undo, 不做软删, 不写审计日志 (避免法律风险) |
| 多端 | user 端实现 | admin 端因不存用户对话数据 (admin 是知识库管理), 暂不开放 |

**关键文件**:

| 文件 | 类型 | 说明 |
|------|------|------|
| `UserPreferenceController.java` | Java Controller | `POST /api/user/preference/forget-me` |
| `UserPreferenceServiceImpl.java` | Java Service | `forgetMe(userId)` 8 步删除 + 事务包裹 |
| `UserPreferenceService.java` | Java Interface | `forgetMe` 方法签名 + Javadoc 范围 |
| `routers/memory.py` | Python 路由 | `POST /chat/memory/forget` (调 clear_facts hard=True) |
| `utils/memory_store.py` | Python 工具 | `clear_facts(settings, user_id, hard=True)` → `delete_by_query` 物理删 ES |
| `api/preference.ts` | 前端 API | `forgetMe()` + `ForgetMeResult` 类型 |
| `PreferencesDialog.vue` | 前端组件 | `forget-me` items 项 + `forgetting` ref + `onForgetMe` (输入 FORGET → 二次确认 → 调 API) |

**端到端测试 (建议)**:
1. 重启 jc-rag-user + jc-rag-service
2. 用测试账号连续多轮对话, 触发 L1 摘要 + L2 提取 + L3 画像
3. 偏好设置 → `被遗忘权 (GDPR)` → `永久删除我的全部数据`
4. 第一次弹窗: 输入 `FORGET` → 下一步
5. 第二次弹窗: 点 `是的, 永久删除`
6. 看到 ElMessage 绿色提示 "已永久删除xxx条数据 (消息x条 / 会话x个 / 摘要x条 / 画像x条 / ES事实x条 / 偏好x项 / 记忆文件x个)"
7. 查 `chat_message / chat_session / chat_session_summary / user_memory_profile / sys_config` → 本用户数据全 0
8. 查 ES `jc_chat_memory` → 本 user_id 文档 0 (硬删不留痕)
9. 查磁盘 `~/.jc/jc-rag/memory/user_profile.md` → 文件不存在 (被删)
10. 用户可重新登录, 账号保留, 只是历史数据全清

### 2.7 L2 矛盾事实处理 (LLM 自动 retract) — **✅ 已完成 (2026-07-03)**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 触发 | 用户在对话中明确撤回/否定/改变某已知 fact | `extract` 端点调 LLM 时, prompt 加 "**矛盾事实处理**" 段落, 引导 LLM 输出 `retract` 数组 |
| 检测 | LLM 自行判断 ("我不用 X 了" / "其实我学的是 Y, 不是 X" / "我换工作了") | prompt 列举 3 个典型句式, 让 LLM 触发 retract |
| 格式 | LLM 输出 `{facts: [...], retract: [{key, value}, ...]}` | `_extract_facts_response` 解析, 容错: 兼容纯 facts 数组 / facts+retract 对象 / 围栏 JSON / 多余 {...} |
| 删除 | 物理删 ES doc (而非软删) | `memory_store.delete_fact_by_value(settings, user_id, key, value)` → `delete_by_query` 按 (user_id, key, value) 精准删 |
| 单独 API | 也提供手动撤回 | `POST /chat/memory/retract` 端点, 业务层调 (目前未前端暴露, 留作扩展) |
| 响应 | 含 `retracted` 字段 | `extract` 响应新增 `retracted: int` 表示物理删条数 |

**关键代码 (Python extract prompt 增段)**:
```
**矛盾事实处理**: 如果用户在对话中明确撤回/否定/改变某已知事实
(如 '我不用 X 了' / '其实我学的是 Y, 不是 X' / '我换工作了'),
请在 `retract` 数组中输出 {key, value} (值与已知事实一致),
系统会物理删除该 fact. 没有矛盾就输出 retract: [].
```

**端到端测试 (建议)**:
1. 重启 jc-rag-service
2. 连续对话触发 L2 提取 (如 "我是 Java 后端")
3. 查 ES `jc_chat_memory` → 有 1 条 `role=Java后端` 的 fact
4. 新对话 "其实我现在转 Go 了, 不用 Java 了"
5. 看后端日志: `chat_memory_extract: LLM returned facts, retracted=X`
6. 查 ES → `role=Java后端` 已被硬删, 可能有 `tech_stack=Go` 新增
7. 下次提问 "我用什么技术栈?" → AI 答 Go (而非 Java)

### 2.8 L0 聊天历史保留策略 (Retention) — **✅ 已完成 (2026-07-03)**

| 项 | 设计 | 实际落地 |
|------|------|----------|
| 触发 | 后台定时任务 | `@Scheduled(cron = "0 0 3 * * ?")` 每天凌晨 3:00 (与 L1 摘要压缩避开) |
| 保留天数 | 配置化, 默认 90 天 | `jc-rag.memory.l0.retention-days: 90` (application-dev.yml) |
| cron | 配置化 | `jc-rag.memory.l0.retention-cron: "0 0 3 * * ?"` |
| 关闭 | 配 0 或负数 | `retentionDays <= 0` → 日志 `disabled`, 跳过本轮 |
| 范围 | L0/L1 数据 | `chat_message` (L0 原文) + `chat_session` (L0 会话) + `chat_session_summary` (L1 摘要) |
| 顺序 | 按依赖关系 | 摘要 → 消息 → 会话 (L1 不依赖任何表, 优先删; 会话最后删, 因其是 message 的父) |
| 事务 | 单事务包裹, 失败回滚 | `@Transactional(rollbackFor = Exception.class)` 整批处理 |
| 索引 | 已有 | `idx_chat_message_created (created_at)` (001 SQL 已建), 无需新建 |
| 手动触发 | 服务层公开方法 | `ChatHistoryRetentionService.cleanup(int days)` 事务方法, 可被管理接口 / 一次性脚本调用 |
| 与 forgetMe 关系 | 互补不冲突 | forgetMe = 用户主动一键清空, retention = 平台级时间窗自动清理 |
| L2/L3 是否清理 | 否 | ES `jc_chat_memory` 与 MySQL `user_memory_profile` 不参与 retention, 由用户主动 forgetMe 处理 (避免破坏记忆连续性) |

**清理 SQL (XML)**:
```xml
<!-- 物理删除, 不可逆; created_at 索引已存在, 全表扫描时间 < 1s (百万级) -->
<delete id="deleteByCreatedAtBefore">
    DELETE FROM chat_message WHERE created_at &lt;= #{cutoff}
</delete>
```

**关键文件**:

| 文件 | 类型 | 说明 |
|------|------|------|
| `ChatMessageMapper.java` + xml | Java Mapper | 加 `deleteByCreatedAtBefore(LocalDateTime cutoff)` |
| `ChatSessionMapper.java` + xml | Java Mapper | 加 `deleteByCreatedAtBefore` |
| `ChatSessionSummaryMapper.java` + xml | Java Mapper | 加 `deleteByCreatedAtBefore` |
| `ChatHistoryRetentionService.java` | Java Service | `@Scheduled` + `cleanup(int days)` 事务方法, `RetentionStat` record 返回 |
| `UserApplication.java` | Java 启动类 | 加 `@EnableScheduling` |
| `application-dev.yml` | Java 配置 | `jc-rag.memory.l0.{retention-days, retention-cron}` |

**端到端测试 (建议)**:
1. 重启 jc-rag-user
2. 准备测试数据: 手动在 MySQL 插入 1 条 `created_at = '2026-01-01'` 的 `chat_message` (90 天前)
3. 修改配置 `retention-days: 89` (调小, 让 90 天前数据落进清理范围)
4. 等待凌晨 3:00 自动执行 (或临时改 cron 为 `0/30 * * * * ?` 每 30 秒一次, 测完改回)
5. 看后端日志: `[L0 retention] done: days=89, messages=1, sessions=0, summaries=0, total=1`
6. 查 MySQL → 测试数据已被物理删除
7. 验证 L2/L3 数据未被影响: ES `jc_chat_memory` 和 MySQL `user_memory_profile` 数据仍在
8. 关闭 retention: 设 `retention-days: 0` → 日志 `disabled, skip`

---

## 三、文件 / 数据库存储 (本阶段已落地)

### 3.1 偏好设置入口 (本期已完成)

**admin 端** (管理员, Step 1a):

| 文件 | 类型 | 说明 |
|------|------|------|
| `AuthToolbar.vue` | 前端组件 | 齿轮下拉菜单**最下方**加"偏好设置" |
| `PreferencesDialog.vue` (admin-web) | 前端组件 | 弹窗, 左右布局 |
| `api/preferences.ts` (admin-web) | 前端 API | `fetchUserPreference` / `updateUserPreference` |
| `stores/preferences.ts` (admin-web) | 前端 Pinia | `useUserPreferenceStore` |
| `UserPreferenceController.java` (admin) | 后端 Controller | `GET/PUT /api/sys/user-preference` |
| `UserPreferenceService.java` + Impl (admin) | 后端 Service | 复用 sys_config 表 |
| `UserPreferenceVO.java` / Param (admin) | 后端 DTO | 字段: `memoryFilePath` |

**user 端** (C 端, Step 1b, 2026-07-02 完成, AI 问答场景的主战场):

| 文件 | 类型 | 说明 |
|------|------|------|
| `ChatHomeView.vue` | 前端组件 | 顶部齿轮下拉菜单**底部**加"偏好设置" (在 API Keys / 页面样式 / 字号 之后, `divided` 分隔) |
| `ProfileView.vue` | 前端组件 | 个人中心新增"偏好设置" card, 按钮"打开设置" |
| `PreferencesDialog.vue` (user-web) | 前端组件 | 弹窗, 左右布局, **self-contained** (不依赖 pinia) |
| `api/preference.ts` (user-web) | 前端 API | `fetchUserPreference` / `updateUserPreference`, 路径 `/user/preference` |
| `UserPreferenceController.java` (user) | 后端 Controller | `GET/PUT /api/user/preference`, 使用 `CoreUserDetails` |
| `UserPreferenceService.java` + Impl (user) | 后端 Service | 复用 user schema 的 `sys_config` 表 |
| `UserPreferenceVO.java` / Param (user) | 后端 DTO | 字段: `memoryFilePath` / `defaultModelCode` / `chatStyle` |

> **两端独立实现**: admin 端和 user 端是独立 Spring Boot + 独立 schema, **不复用** (跨模块依赖会破坏分层)。代码结构 1:1 镜像, 存储约定一致 (`group=user_pref`)。

### 3.2 偏好设置项 (本期实现情况)

| 设置项 | admin 端 | user 端 |
|--------|---------|---------|
| 记忆文件位置 (memoryFilePath) | **已实现** | **已实现** |
| 默认模型 (defaultModelCode) | 占位 | **已实现** (user 端独有) |
| 回答风格 (chatStyle) | 占位 | **已实现** (user 端独有) |
| 主题外观 | 占位 | 占位 |
| 界面语言 | 占位 | 占位 |
| 字号与排版 | 占位 | 占位 |
| 快捷键 | 占位 | 占位 |
| 消息通知 | 占位 | 占位 |
| 历史保留时长 | 占位 | 占位 |
| 使用统计与报告 | 占位 | 占位 |

### 3.3 存储约定

复用 `sys_config` 表, 避免新增表:
- `config_group` = `user_pref`
- `config_key` = `{userId}.{preferenceKey}` (如 `1001.memoryFilePath`)
- `config_value` = 偏好值字符串
- `value_type` = `STRING` / `NUMBER` / `BOOLEAN`

读取走 `QueryWrapper` 按 `(group, key)` 精准匹配, 不依赖 `SysConfigMapper.selectStringValue` (后者不传 group, 跨分组时存在误命中风险)。

### 3.4 默认值

| Key | 默认值 | 适用端 | 说明 |
|-----|--------|--------|------|
| `memoryFilePath` | `~/.jc/jc-rag/memory/user_profile.md` | admin + user | 用户主目录下, 跨平台 |
| `defaultModelCode` | `""` (空 = 跟随系统默认) | user 端独有 | C 端用户在 chat 时选模型 |
| `chatStyle` | `concise` (简洁) | user 端独有 | 选项: concise / detailed / friendly / professional |

**`~` 展开规则** (后续 L2 落地时统一处理):
- Linux/macOS: `System.getProperty("user.home")`
- Windows: `System.getProperty("user.home")` (即 `C:\Users\xxx`)

### 3.5 记忆文件管理 (本期已完成, 2026-07-02)

**目标**: 让偏好设置中的"记忆文件位置"**真实可用** (而非仅字符串占位). 用户能在 Web 端查看 / 编辑 / 复制路径 / (Chrome/Edge) 在系统中打开.

#### 3.5.1 后端 (user 端)

| 文件 | 作用 |
|------|------|
| `MemoryFileVO.java` | 视图: rawPath / resolvedPath / parentDir / content / exists / bytes / lastModified / autoInitialized |
| `MemoryFileSaveParam.java` | 入参: content (允许空串, 限 1MB) |
| `UserPreferenceService.readMemoryFile(userId)` | 读取: 文件不存在则**自动初始化**默认模板到磁盘 |
| `UserPreferenceService.writeMemoryFile(userId, param)` | 保存: 父目录自动 `mkdir -p`, **原子写** (临时文件 → rename) |
| `UserPreferenceController` | `GET/PUT /api/user/preference/memory-file` |

**关键技术点**:
- 路径解析: `~` → `System.getProperty("user.home")` (Windows: `C:\Users\xxx`)
- 跨平台: 用 `java.nio.file.Path` / `Paths.get(String)` 自动适配分隔符
- 父目录自动建: `Files.createDirectories(parent)`
- 原子写: `Files.writeString(tmp) → Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`, 失败降级为普通 move
- 默认模板: 首次保存写入, 包含"技术背景 / 工作偏好 / 长期目标"三段
- 错误处理: IOException → `IllegalStateException` → 触发全局异常 → 友好提示

#### 3.5.2 前端 (user-web)

| 文件 | 作用 |
|------|------|
| `api/preference.ts` | 加 `fetchMemoryFile` / `saveMemoryFile` 两个 API + `MemoryFileVO` 接口 |
| `PreferencesDialog.vue` | 4 个操作按钮 + 嵌套的"查看/编辑"弹窗 |

**4 个操作按钮** (横向排列在"操作" el-form-item 下):

| 按钮 | 行为 | 适用浏览器 |
|------|------|----------|
| 查看文件 | 调 GET → 弹只读编辑器 | 全部 |
| 编辑并保存 | 调 GET → 弹可写编辑器 → PUT 写盘 | 全部 |
| 复制路径 | `navigator.clipboard.writeText` | 全部 (降级: 提示手动复制) |
| 在系统中打开 | File System Access API (`showDirectoryPicker`) | **仅 Chrome 86+ / Edge** |

**浏览器限制说明** (核心认知):
- Web 应用**无法**直接打开用户本地任意目录 (浏览器安全沙箱, 所有浏览器厂商都限制)
- File System Access API 让用户**主动授权选目录**, **不能**自动定位 `~/.jc/jc-rag/memory/`
- 对"非 Chrome/Edge 浏览器"或"不想授权"的用户, 折中方案: 复制路径 → `Win+R` (Windows) / `Cmd+Space` (macOS) / 文件管理器地址栏 (Linux) 粘贴打开

**用户视角真实体验** (本地部署):
1. 用户在偏好设置点"编辑并保存" → 后端真的把 .md 写到 `C:\Users\xxx\.jc\jc-rag\memory\user_profile.md`
2. 用户在资源管理器真的能看到这个文件被创建/修改
3. L2 阶段 (长期事实) 启动时, 后端**直接读这个文件**做 AI 记忆库

**云端部署说明**:
- 此功能在**云端部署**下, 文件落在云端服务器磁盘 (不是用户本地)
- 对云端用户, 应改用云端存储 (数据库 / 对象存储) 而非本地文件
- 本期暂不区分, 部署文档后续会注明此限制

#### 3.5.3 端到端测试

```bash
# 1. 重启 jc-rag-user
# 2. 浏览器 → 登录 → ChatHomeView 顶部齿轮 → "偏好设置" → "记忆文件位置"
# 3. 点 "编辑并保存" → 应自动看到默认模板 (提示: "已自动创建默认模板")
# 4. 改点内容 → 点 "保存到磁盘" → 提示 "已保存 (xxx 字节)"
# 5. 资源管理器打开 C:\Users\<你的用户名>\.jc\jc-rag\memory\ → 真的能看到 user_profile.md 文件
# 6. 用记事本打开验证内容是否一致
```

---

## 四、与国际大厂对比

| 能力 | Trae/Cursor | ChatGPT | Mem0 | 本方案 |
|------|-------------|---------|------|--------|
| 短期记忆 | ✅ | ✅ | ✅ | ✅ L0 |
| 长期事实 | ✅ (项目级) | ✅ (用户级) | ✅ (核心) | ✅ L2 |
| 用户画像 | ❌ | ✅ (粗粒度) | ❌ | ✅ L3 |
| 摘要压缩 | ✅ | ✅ | ✅ | ✅ L1 |
| 用户可控 | ✅ | ✅ | ✅ | ✅ |
| 跨平台 | IDE | Web/APP | API | Web/APP |
| 关键事实而非全回放 | ✅ | ✅ | ✅ | ✅ L2 |

**结论**: 在"RAG 对话记忆"维度, 本方案**对齐或接近国际大厂水平**, 已完成 L0 + L1 + L2, 剩余 L3 跨会话画像 (可选)。

---

## 五、商业化策略 (待办)

### 5.1 边界

| 范畴 | 开源 (核心) | 商业 (待定) |
|------|-------------|------------|
| 短期/摘要记忆 | ✅ 全部 | - |
| 长期事实 (个人) | ✅ ≤50 条 | 无限制 + 高级 |
| 长期事实 (团队) | ❌ | ✅ 团队共享 |
| 长期事实 (组织) | ❌ | ✅ 全公司共享 |
| 主动指令 (记住/忘掉) | ❌ | ✅ |
| 时间轴回忆录 | ❌ | ✅ |
| 跨平台导入 (Cursor/Notion) | ❌ | ✅ |
| 多模态记忆 (图片/PDF) | ❌ | ✅ |

### 5.2 待办

商业版形态 (个人专业版 / 团队版 / 企业版) 尚未确定, **待与产品进一步商讨后启动**。
当前阶段: **核心记忆功能开源, 商业化作为后续专项**。

---

## 六、API 契约草案 (L0 ~ L2 完整规划)

### 6.1 Java 侧

```java
// 新增: ChatAskParam
public class ChatAskParam {
    private String question;
    private List<Attachment> attachments;
    private Boolean enableLongTermMemory = true;  // 用户可关
    private Integer historyLimit;                 // 自定义窗口
}

// 新增: MemoryFactVO
@Data
public class MemoryFactVO {
    private String memoryId;
    private String key;
    private String value;
    private String category;
    private Double score;        // 相似度
    private LocalDateTime createdAt;
}

// 新增: ChatAskMetaVO (随响应返回, 让前端展示 "AI 想起了什么")
public class ChatAskMetaVO {
    private List<MemoryFactVO> recalledMemories;
    private Integer historyUsedRounds;
    private Integer totalTokens;
}
```

### 6.2 Python 侧

```python
# ChatAskRequest
class HistoryMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str

class ChatAskRequest(BaseModel):
    kbId: int | str | None = None
    question: str
    history: list[HistoryMessage] = []
    memoryFacts: list[dict] = []
    enableLongTermMemory: bool = True
    # ... 既有字段
```

### 6.3 新增 Controller 端点

```
GET    /api/sys/memory/facts          # 列出当前用户事实
DELETE /api/sys/memory/facts          # 清空当前用户事实
DELETE /api/sys/memory/facts/{id}     # 删单条
GET    /api/sys/memory/summaries      # 当前会话的摘要
```

---

## 七、Token 经济测算

| 记忆层 | 触发 | 占用 token/轮 |
|--------|------|----------------|
| 系统 prompt (含 RAG 提示词) | 必带 | ~500 |
| L2 事实 (5 条 × 30 字) | 必带 | ~200 |
| L0 短期窗口 (10 轮) | 必带 | ~2000 |
| L1 摘要 (1-2 段) | 必带 | ~300 |
| 当前问题 | 必带 | ~200 |
| **合计每轮** | | **~3200 tokens** |
| **100 轮累计存储** | | **~30K tokens 存储 (DB)** |

**关键**: 不是 100 轮全喂 LLM, 是"100 轮都存着, 但每轮只喂 3200 tokens"。

---

## 八、风险与陷阱

| 风险 | 缓解 |
|------|------|
| 事实爆炸 (3 个月后 500 条) | 检索 topK=5 + threshold=0.6, 总量不限但检索只看最相关 |
| 旧事实失效 (技术栈变更) | LLM 提取时自动 update 相似事实 (>0.9 触发); 手动 DELETE 兜底 |
| LLM 提取质量差 | 严格 prompt 限制 (≤5 条, 明确提取标准); 失败重试 3 次后降级 |
| 敏感信息泄漏 | 提取 prompt 显式排除 (身份证/密码/手机号); 前端"敏感过滤"开关 |
| 嵌入模型升级旧向量失效 | 嵌入模型变更时**全量重 embed** 一次性脚本 |
| GDPR / 个保法 法律风险 | 一键忘记我 (`forgetMe`) 物理删除 L0+L1+L2+L3+偏好+记忆文件, 不可逆, 不留痕; 二次确认 (输 `FORGET` 三个字母) |
| 矛盾事实残留 (用户改口) | L2 提取 prompt 加 `retract` 字段, LLM 自动检测"我不用 X 了"等句式, 物理删除旧 fact |
| L0 数据无界增长 | 后台 `@Scheduled` retention 每天 03:00 物理删除 N 天前 (默认 90) 的 L0/L1 数据; 配 0/负数即关闭 |
| retention 误删重要数据 | retention 默认 90 天, 时间窗足够长; 仅删 L0/L1, 不碰 L2/L3 (用户记忆连续性); 配置可调 |
| retention 锁表阻塞业务 | 单条 `DELETE` 走 `idx_chat_message_created` 索引, 百万级秒级完成; 失败整体回滚 |
| **Element Plus icon 引用错误** (已踩坑) | 引入 element-plus icon **必须**用 PowerShell 提取 `dist/index.js` 中 `_default as PascalName` 行作为真实导出清单; ❌ 用 `name: "Xxx"` grep 会误命中 `XxxFilled` 等导致实际不存在 |
| **Edit 工具"假成功"陷阱** (已踩坑) | Edit 工具返回 "updated successfully" 但实际未改 (尤其涉及缩进/字符差异时); 任何 Edit 后**必须 grep 验证**文件状态, 不再信任工具的"成功"反馈 |

---

## 九、落地顺序

| 阶段 | 内容 | 工作量 | 阻塞依赖 | 状态 |
|------|------|--------|----------|------|
| **Step 1** | 偏好设置入口 + 记忆文件位置 | 0.5d | 无 | ✅ 2026-07-02 完成 |
| **Step 2 (L0)** | 短期滑动窗口 (历史传 Python) | 2-3d | 无 | ✅ 2026-07-03 完成 |
| **Step 3 (L1)** | 摘要压缩表 + 异步任务 | 2-3d | L0 | ✅ 2026-07-03 完成 |
| **Step 4 (L2)** | 事实提取 + ES 启用 jc_chat_memory | 4-5d | L0 | ✅ 2026-07-03 完成 |
| **Step 5 (L3)** | 用户画像 (聚合) | 1-2d | L2 | ✅ 2026-07-03 完成 |
| **Step 6 (P0-1)** | 一键忘记我 (GDPR / 个保法) | 0.5d | L0+L1+L2+L3 | ✅ 2026-07-03 完成 |
| **Step 7 (P0-2)** | L2 矛盾事实处理 (LLM retract) | 0.3d | L2 | ✅ 2026-07-03 完成 |
| **Step 8 (P0-3)** | L0 chat_message retention (后台定时清理) | 0.3d | L0 | ✅ 2026-07-03 完成 |
| **Step 9 (商业)** | 商业版差异化 (待商讨) | 待定 | 商业策略定 | ⏳ 待启动 |

**每阶段独立可用、独立可测**。每完成一阶段, 在 `7月2日代办.md` 中更新进度。

---

## 十、变更日志

- 2026-07-02: 初稿, 记录偏好设置入口 + 整体分层设计
- 2026-07-02: 完成 Step 1b (user 端) — 偏好设置入口 + 3 个实现项 (记忆文件位置 / 默认模型 / 回答风格); 双端入口 (ChatHomeView 顶部齿轮 + ProfileView card)
- 2026-07-03: 完成 L0 短期记忆 — Java 端读 chat_message 拼 history, Python 端 /chat/ask/stream + /chat/ask 注入 messages 数组; 用户体验: 多轮对话上下文连续
- 2026-07-03: 完成 L1 摘要压缩 — 建表 chat_session_summary, Java 异步任务 (20 轮触发), Python /chat/summary 端点, 摘要注入 systemPrompt 头部, 失败 3 次降级为前 5 轮拼接, 同会话再次压缩时旧摘要自动失效
- 2026-07-03: 完成 L2 长期事实 — 建表 (无新增, 复用 ES jc_chat_memory), Java 异步 fact 提取 + 召回注入, Python /chat/memory/* 5 端点, 偏好开关 + 我的记忆页
- 2026-07-03: 完成 L3 跨会话画像 — 建表 user_memory_profile (SQL 023), Java 实体/Mapper/Service/Controller/Controller全套, Python /chat/profile/extract, 前端 MyMemoryView 顶部大卡 + 5 段彩色徽章, 偏好开关, profileTriggerRounds 走 application.yml 配置化 (默认 10 轮)
- 2026-07-03: 完成 P0-1 一键忘记我 (GDPR / 个保法 被遗忘权) — Java Service 8 步物理删除 (L0/L1/L3/偏好/记忆文件) + Java HttpClient 调 Python `/chat/memory/forget` 物理删 ES; Python `_extract_facts_response` 解析 `{facts, retract}` 结构; 前端 PreferencesDialog 加 `被遗忘权 (GDPR)` 项 + 输入 `FORGET` 二次确认 + onForgetMe 弹窗流程; 不可逆
- 2026-07-03: 完成 P0-2 L2 矛盾事实处理 — Python `extract` prompt 加 "**矛盾事实处理**" 段, 引导 LLM 输出 `retract: [{key, value}]` 数组; `_extract_facts_response` 容错解析 (兼容纯数组/对象/围栏 JSON); `memory_store.delete_fact_by_value` 精准按 (user_id, key, value) 物理删 ES doc; 新增 `POST /chat/memory/retract` 端点 (留作扩展)
- 2026-07-03: 完成 P0-3 L0 chat_message retention — 3 个 Mapper (chat_message / chat_session / chat_session_summary) 加 `deleteByCreatedAtBefore`; `ChatHistoryRetentionService` 用 `@Scheduled(cron)` 每天 03:00 物理删 N 天前数据 (默认 90 天); `UserApplication` 加 `@EnableScheduling`; `application-dev.yml` 加 `jc-rag.memory.l0.{retention-days, retention-cron}`; 公开 `cleanup(int days)` 事务方法供手动触发; 仅清 L0/L1, 不碰 L2/L3
