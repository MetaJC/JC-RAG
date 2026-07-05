# 单条测试用例模板

> Agent 解析此格式. 一个文件 (`test-cases.md`) 可包含 1-N 条用例, 用 `## TC-...` 二级标题分隔.

## 模板 (复制本块)

```markdown
## TC-{FEATURE_ID}-{NNN} 简明标题

- **ID**: TC-MEM-L0-001
- **Feature**: MEM-L0
- **Type**: e2e | api | unit | ui | security
- **Priority**: P0 | P1 | P2
- **Status**: draft | ready | pass | fail | blocked
- **CreatedAt**: 2026-07-03
- **LastVerified**: (空, 测试后由 Agent 填)
- **Author**: JC
- **CodeRefs**:
  - backend: `jc-rag-backend/jc-rag-user/src/main/java/.../UserChatServiceImpl.java#buildShortTermHistory`
  - frontend: `jc-rag-frontend/jc-rag-user-web/src/views/user/ChatHomeView.vue`
  - api: `POST /api/chat/ask/stream`
- **Related**: [TC-MEM-L0-002](#tc-mem-l0-002), [TC-MEM-L0-005](#tc-mem-l0-005)
- **PreConditions**:
  - 用户 `test_user_01` 已登录 (Cookie: `JC_SESSION=...`)
  - jc-rag-user (port 8082) 运行中
  - jc-rag-service (port 8001) 运行中
  - 用户已有 5 条历史对话 (见 `data/users.md#test_user_01`)
- **Steps**:
  1. 打开 chat 页 (`http://localhost:8082/chat`)
  2. 发送消息 1: "我最喜欢的编程语言是 Python"
  3. 等待 AI 回复完成 (SSE 结束事件 `done`)
  4. 发送消息 2: "我叫什么名字"
  5. 等待 AI 回复完成
  6. ... (累计 10 轮)
  7. 发送消息 11: "我最喜欢什么编程语言"
  8. 等待 AI 回复完成
- **Expected**:
  - 消息 11 的 AI 回复文本中包含 "Python"
  - SSE 事件流包含 `delta` 事件, 且 `delta.content` 非空
  - chat_message 表新增 11 条记录 (user 5 条, assistant 6 条)
- **AutoCheck** (YAML, Agent 执行):
  ```yaml
  steps:
    - send_message: "我最喜欢的编程语言是 Python"
      expect_reply_contains: "Python"
    - send_message: "我叫什么名字"
      # 此条不验证内容, 仅记录
    - send_messages_repeat: 8   # 累计到 10 轮
    - send_message: "我最喜欢什么编程语言"
      expect_reply_contains: "Python"
  db_assert:
    - table: chat_message
      where: { user_id: test_user_01, created_at: now-5min }
      count: 11
  ```
- **Actual**: (Agent 测试后填, 含耗时)
- **Notes**:
  - L0 滑动窗口默认 10 轮
  - Token 预算 4000 (配置项 `jc-rag.memory.l0.token-budget`)
  - 跨会话场景见 TC-MEM-L0-002
```

## 字段规约 (Agent 解析用)

| 字段 | 必填 | 类型 | 说明 |
|------|------|------|------|
| ID | ✓ | str | 全局唯一, `TC-{FEATURE_ID}-{NNN}` |
| Feature | ✓ | str | 功能 ID, 用于路由 |
| Type | ✓ | enum | e2e/api/unit/ui/security |
| Priority | ✓ | enum | P0 (核心) / P1 (重要) / P2 (次要) |
| Status | ✓ | enum | draft/ready/pass/fail/blocked |
| CodeRefs | ✗ | obj | 代码位置, Agent 验证时定位 |
| PreConditions | ✗ | list | 步骤前环境, Agent 自动 setup |
| Steps | ✓ | list | 步骤描述 (人读) |
| Expected | ✓ | list | 期望结果 (人读) |
| AutoCheck | ✗ | yaml | Agent 自动验证脚本 (机读) |
| Actual | ✗ | str | Agent 测试后填 |

## AutoCheck YAML 规范

### 支持的 step 类型

```yaml
- send_message: "消息内容"           # 发送聊天消息
  expect_reply_contains: "关键词"    # 期望回复包含
  expect_reply_contains_all: ["A","B"]  # 全部包含
  expect_reply_not_contains: "X"     # 不包含

- api_call:                          # 调 REST API
    method: POST
    url: /api/chat/summary
    body: { sessionId: "${sessionId}" }
  expect_status: 200
  expect_body_contains: "summary"

- db_query:                          # 直查 MySQL
    sql: SELECT COUNT(*) FROM chat_message WHERE user_id = ? AND created_at > ?
    params: [test_user_01, "2026-07-03 00:00:00"]
  expect: 11

- es_query:                          # 查 ES
    index: jc_chat_memory
    body: { query: { term: { user_id: "test_user_01" } } }
  expect_hits: 5

- file_check:                        # 检查磁盘文件
    path: ${user.home}/.jc/jc-rag/memory/user_profile.md
    exists: true
    content_contains: "Python"

- wait_event:                        # 等待 SSE 事件
    type: done
    timeout: 30s
```

### 变量替换

- `${sessionId}` / `${userId}` — Agent 运行时从上下文替换
- `${user.home}` — 系统用户目录
- `${now-5min}` — Agent 测试时间锚定

## 状态机

```
draft → ready (开发者确认) → pass/fail (Agent 测试) → blocked (环境问题)
                ↑                       ↓
                └───── (修复后重跑) ─────┘
```

## 示例: 一条 P0 用例 (用于 Agent 调试协议)

见 [../features/MEM-L0/test-cases.md](../features/MEM-L0/test-cases.md) (本轮交付后补).
