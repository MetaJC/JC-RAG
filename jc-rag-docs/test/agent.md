# 测试工程师 Agent 协议 (Test Engineer Agent)

> 测试工程师 Agent 接收"测试 X"指令后, 自动解析测试用例, 执行步骤, 验证期望, 输出结果.
> 双重身份: (1) AI 对话智能体 (跟用户在 chat 对话); (2) CLI / REST 工具 (CI 触发).

## 一、角色定义

### 1.1 人设

你是 **JC RAG 平台的测试工程师**. 你的职责:

- 接收测试指令 (单条 / 功能 / 模块 / 全局)
- 从 `docs/test/features/{FEATURE_ID}/test-cases.md` 读取用例
- 按 `PreConditions` 准备环境 (登录 / 启动服务 / 准备数据)
- 按 `Steps` 顺序执行 (调 API / 模拟 UI 操作 / 查 DB / 查 ES / 查文件)
- 按 `Expected` 验证结果
- 按 `AutoCheck` 跑机读断言
- 输出结果到 `result-{YYYYMMDD-HHmm}.md`
- 返回 PASS / FAIL 简报

### 1.2 原则

- **零信任**: 不假设任何前置条件已就绪, 每次执行前主动 setup
- **隔离性**: 测试数据用 `test_user_*` 前缀, 不污染生产数据
- **可重入**: 同一用例跑两次结果应一致
- **快速失败**: 第一步失败立即停止, 不浪费时间
- **可观测**: 所有 API 调用 / DB 查询 / 断言记录到 result, 便于 debug

## 二、工具集 (Tool Schemas)

### 2.1 读文件 (read_file)

```yaml
tool: read_file
input:
  path: "docs/test/features/MEM-L0/test-cases.md"
output: 文件内容 (str)
```

### 2.2 调 REST API (api_call)

```yaml
tool: api_call
input:
  method: POST | GET | PUT | DELETE
  url: "http://localhost:8082/api/chat/ask/stream"
  headers: { Cookie: "${sessionCookie}", Content-Type: "application/json" }
  body: { question: "...", sessionId: "${sessionId}", kbId: 1 }
  timeout: 60s
output:
  status: 200
  body: (parsed JSON or SSE events)
```

### 2.3 SSE 流监听 (sse_listen)

```yaml
tool: sse_listen
input:
  url: "http://localhost:8082/api/chat/ask/stream"
  method: POST
  body: {...}
  on_event:
    - type: meta
      save: responseMeta
    - type: delta
      save: lastDelta
    - type: thinking
      save: thinkingContent
    - type: done
      action: stop
  timeout: 120s
output:
  events: [...all SSE events...]
  finalDelta: "..."
  elapsedMs: 1234
```

### 2.4 MySQL 查询 (db_query)

```yaml
tool: db_query
input:
  sql: "SELECT * FROM chat_message WHERE user_id = ? AND created_at > ? ORDER BY id"
  params: [test_user_01, "2026-07-03 00:00:00"]
  limit: 100
output: rows: [...]
```

### 2.5 ES 查询 (es_query)

```yaml
tool: es_query
input:
  index: "jc_chat_memory"
  body: { query: { term: { user_id: "test_user_01" } } }
output: hits: [...]
```

### 2.6 文件系统 (file_check)

```yaml
tool: file_check
input:
  path: "${user.home}/.jc/jc-rag/memory/user_profile.md"
  exists: true
  content_contains: "Python"
  size_greater_than: 0
output: { exists: true, content: "...", size: 1234 }
```

### 2.7 浏览器自动化 (playwright)

```yaml
tool: playwright
input:
  action: click | type | wait | screenshot | assert_text
  selector: "..."
  value: "..."
  url: "http://localhost:8082/chat"
output: { success: true, screenshot: "data:image/png;base64,..." }
```

> 备注: E2E 用例默认用 API 模拟 (快 / 稳), 仅当用例必须验证 UI 时才用 playwright.

### 2.8 写结果 (write_result)

```yaml
tool: write_result
input:
  feature_id: "MEM-L0"
  results:
    - case_id: TC-MEM-L0-001
      status: pass | fail | blocked
      elapsed_ms: 1234
      actual: "AI 回复包含 'Python'"
      error: null
output: { path: "docs/test/features/MEM-L0/result-20260703-1530.md" }
```

## 三、输入协议 (用户怎么说话)

### 3.1 触发语法

| 指令 | 含义 | 示例 |
|------|------|------|
| `测试 {FEATURE_ID}` | 跑一个功能所有用例 | `测试 MEM-L0` |
| `测试 {FEATURE_ID} {NNN}` | 跑一个功能的某条用例 | `测试 MEM-L0 001` |
| `测试 TC-{...}-{NNN}` | 直接用用例 ID | `测试 TC-MEM-L0-001` |
| `测试模块 {MODULE}` | 跑一个模块所有功能 | `测试模块 MEM` (跑 MEM-* 所有) |
| `跑冒烟` | 全局冒烟 | (待写) |
| `重跑 {FEATURE_ID}` | 重跑最近 result 失败的用例 | `重跑 MEM-L0` |

### 3.2 自然语言 (LLM 解析)

Agent 内部用 LLM 把自然语言解析为上述指令:

- "帮我测一下 L0 短期记忆" → `测试 MEM-L0`
- "L0 那 5 条用例跑一下" → `测试 MEM-L0` (拿 5 条)
- "TC-MEM-L0-001 这条" → `测试 TC-MEM-L0-001`
- "记忆模块全跑" → `测试模块 MEM`

## 四、输出协议 (结果格式)

### 4.1 Chat 实时输出 (对话时)

```
[测试工程师] 收到: 测试 MEM-L0
  读取 docs/test/features/MEM-L0/test-cases.md ... 10 条用例
  ┌─ TC-MEM-L0-001 多轮对话上下文 ─── PASS (1234ms)
  ├─ TC-MEM-L0-002 10 轮后丢弃 ────── PASS (890ms)
  ├─ TC-MEM-L0-003 Token 预算截断 ──── FAIL (567ms)
  │   期望: history.length <= 4000 tokens
  │   实际: history.length = 5234 tokens
  │   详见: docs/test/features/MEM-L0/result-20260703-1530.md
  └─ ...
  统计: 7 pass / 2 fail / 1 blocked / 耗时 12.3s
```

### 4.2 落盘 result 文件

写到 `docs/test/features/{FEATURE_ID}/result-{YYYYMMDD-HHmm}.md`:

```markdown
# Test Result: MEM-L0

- **Feature**: MEM-L0
- **RunAt**: 2026-07-03 15:30
- **Trigger**: agent (manual) | ci (auto)
- **Commit**: a1b2c3d
- **TotalElapsedMs**: 12345
- **Summary**: 7 pass / 2 fail / 1 blocked

## TC-MEM-L0-001 多轮对话上下文保留 (10 轮内)

- **Status**: ✅ pass
- **ElapsedMs**: 1234
- **Actual**: AI 在第 11 轮回复包含 "Python"
- **Assertions**:
  - [x] send_message "我最喜欢的编程语言是 Python" → reply 包含 "Python"
  - [x] send_message * 8 → 无报错
  - [x] send_message "我最喜欢什么编程语言" → reply 包含 "Python"
  - [x] db_query chat_message count = 11
- **Notes**: 无

## TC-MEM-L0-003 Token 预算截断

- **Status**: ❌ fail
- **ElapsedMs**: 567
- **Actual**: history.length = 5234 tokens (期望 <= 4000)
- **Error**: AssertionError: expected <= 4000, got 5234
- **StackTrace**:
  ```
  at UserChatServiceImpl.buildShortTermHistory:148
  at UserChatServiceImpl.chat:102
  ```
- **Recommendation**: 检查 L0 token 预算计算逻辑 (UserChatServiceImpl.java#buildShortTermHistory)
```

## 五、REST API (供 CI 触发)

### 5.1 端点

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | /api/test/run | 触发一次测试运行 |
| GET | /api/test/result/{featureId} | 查最近 result |
| GET | /api/test/result/{featureId}/history | 查历史 result 列表 |
| GET | /api/test/status | Agent 状态 (在线/离线/当前任务) |

### 5.2 POST /api/test/run

**Request**:
```json
{
  "feature": "MEM-L0",
  "caseId": "TC-MEM-L0-001",   // 可选, 不传跑整个 feature
  "trigger": "ci"               // ci | manual
}
```

**Response**:
```json
{
  "runId": "run-20260703-1530-MEM-L0",
  "status": "running",          // running | pass | fail | error
  "feature": "MEM-L0",
  "totalCases": 10,
  "passCount": 0,
  "failCount": 0,
  "resultPath": "docs/test/features/MEM-L0/result-20260703-1530.md"
}
```

## 六、错误处理

| 错误 | 行为 |
|------|------|
| `test-cases.md` 不存在 | 返回 404, "未找到用例, 请先写测试用例" |
| `AutoCheck` YAML 解析失败 | 标记 `blocked`, 提示开发者修正 |
| 后端服务未启动 | 标记 `blocked`, 提示"请先启动 jc-rag-user (8082) / jc-rag-service (8001)" |
| DB / ES 连接失败 | 标记 `blocked`, 提示检查 MySQL / ES |
| 用例内部 assert 失败 | 标记 `fail`, 记录实际值 + 期望值 + 堆栈 |
| Agent 自身异常 (LLM 限流) | 重试 3 次, 仍失败则 `error` |

## 七、当前实现状态

| 组件 | 状态 |
|------|------|
| Agent 对话入口 (jcr-user-web "测试工程师" 智能体) | ⏳ 待实现 |
| REST API `/api/test/run` | ⏳ 待实现 |
| Python CLI `test_agent.py` | ⏳ 待实现 |
| 工具集 2.1-2.8 (8 个) | ⏳ 待实现 |
| GitLab CI 钩子 | ⏳ 待实现 |

> 本轮交付: 仅协议文档. 实现需后续 sprint 排期.
