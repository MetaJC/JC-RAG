# Feature: MEM-L0 - L0 短期记忆 (滑动窗口)

- **Feature ID**: MEM-L0
- **Name**: L0 短期记忆
- **Module**: memory
- **Priority**: P0
- **CreatedAt**: 2026-07-03
- **Author**: JC
- **CodeRefs**:
  - backend: `jc-rag-backend/jc-rag-user/src/main/java/com/jc/rag/user/module/chat/service/impl/UserChatServiceImpl.java#buildShortTermHistory`
  - python: `jc-rag-service/app/api/routers/chat.py#stream_generate`
  - frontend: `jc-rag-frontend/jc-rag-user-web/src/views/user/ChatHomeView.vue`
  - api: `POST /api/chat/ask/stream`
- **ConfigRefs**:
  - `application.yml: jc-rag.memory.l0.history-rounds=10` (默认 10 轮)
  - `application.yml: jc-rag.memory.l0.token-budget=4000`
- **PreConditions** (全局):
  - test_user_01 已登录
  - jc-rag-user (8082) / jc-rag-service (8001) 启动
  - MySQL / ES / Redis 健康
  - 已有 1 个空 session (id = ${sessionId})
- **PostConditions**:
  - 删除本 feature 产生的 chat_message (where session_id = ${sessionId})
  - 删除本 feature 产生的 L1 summary (where session_id = ${sessionId})
- **TotalCases**: 10

---

## TC-MEM-L0-001 多轮对话上下文保留 (10 轮内)

- **ID**: TC-MEM-L0-001
- **Type**: e2e
- **Priority**: P0
- **Status**: ready
- **PreConditions**:
  - test_user_01 已登录
  - 空 session ${sessionId}
- **Steps**:
  1. 发送消息 1: "我最喜欢的编程语言是 Python"
  2. 等待 AI 回复完成
  3. 发送消息 2: "我叫什么名字"
  4. ... 累计 10 轮
  5. 发送消息 11: "我最喜欢什么编程语言"
  6. 等待 AI 回复
- **Expected**:
  - 消息 11 的 AI 回复文本中包含 "Python"
- **AutoCheck**:
  ```yaml
  steps:
    - send_message: "我最喜欢的编程语言是 Python"
    - repeat_send: 9   # 凑足 10 轮
    - send_message: "我最喜欢什么编程语言"
      expect_reply_contains: "Python"
  ```
- **Notes**: L0 默认保留 10 轮, 10 轮内引用有效

---

## TC-MEM-L0-002 10 轮后上下文丢弃

- **ID**: TC-MEM-L0-002
- **Type**: e2e
- **Priority**: P0
- **Status**: ready
- **PreConditions**: 同 TC-MEM-L0-001
- **Steps**:
  1. 发送消息 1: "我最喜欢的编程语言是 Python"
  2. ... 累计 11 轮
  3. 发送消息 12: "我最喜欢什么编程语言"
- **Expected**:
  - 消息 12 的 AI 回复**不**包含 "Python" (或表示"不知道")
  - (L0 窗口仅 10 轮, 第 11 轮时消息 1 已被丢弃)
- **AutoCheck**:
  ```yaml
  steps:
    - send_message: "我最喜欢的编程语言是 Python"
    - repeat_send: 10
    - send_message: "我最喜欢什么编程语言"
      expect_reply_not_contains: "Python"
      expect_reply_contains_any: ["不知道", "请告诉我", "未提及"]
  ```

---

## TC-MEM-L0-003 Token 预算截断

- **ID**: TC-MEM-L0-003
- **Type**: api
- **Priority**: P1
- **Status**: ready
- **PreConditions**:
  - session 中已有 20 轮长对话 (单条 1000 tokens, 累计 20000 tokens)
- **Steps**:
  1. 调 `POST /api/chat/ask/stream` 发送新消息
  2. 拦截 Python 端, 记录传给的 `messages` 数组总 token 数
- **Expected**:
  - `messages` 数组总 token <= 4000 (配置 `token-budget=4000`)
  - 最早的消息被截断, 保留最新的
- **AutoCheck**:
  ```yaml
  api_call:
    method: POST
    url: /api/chat/ask/stream
    body: { sessionId: ${sessionId}, question: "测试" }
  assert:
    - type: interceptor
      hook: python_stream_generate
      field: messages_total_tokens
      lte: 4000
  ```

---

## TC-MEM-L0-004 不同 session 独立上下文

- **ID**: TC-MEM-L0-004
- **Type**: e2e
- **Priority**: P0
- **Status**: ready
- **PreConditions**:
  - test_user_01 已登录
  - sessionA (id=1) 和 sessionB (id=2) 都为空
- **Steps**:
  1. sessionA 发送 "我喜欢 Java"
  2. sessionB 发送 "我喜欢的编程语言?"
  3. 等待 AI 回复 (sessionB)
- **Expected**:
  - sessionB 的 AI 回复**不**包含 "Java" (上下文隔离)
  - 引用 sessionA 内容会失败
- **AutoCheck**:
  ```yaml
  steps:
    - session: A
      send: "我喜欢 Java"
    - session: B
      send: "我喜欢的编程语言?"
      expect_reply_not_contains: "Java"
  ```

---

## TC-MEM-L0-005 SSE 流式期间上下文正确

- **ID**: TC-MEM-L0-005
- **Type**: api
- **Priority**: P0
- **Status**: ready
- **Steps**:
  1. 发送消息 1: "解释一下 Java 的 volatile 关键字"
  2. 监听到第一个 `delta` 事件时, 立即发送消息 2: "volatile 和 synchronized 区别?"
  3. 验证两条消息的 AI 回复都正确 (不丢不串)
- **Expected**:
  - 消息 1 的 delta 流完整生成 AI 回复
  - 消息 2 不被消息 1 截断
  - chat_message 表新增 2 条 user + 2 条 assistant
- **AutoCheck**:
  ```yaml
  sse_listen:
    - first_event: delta
      trigger: send_message "volatile 和 synchronized 区别?"
    - assert: 两条消息的 SSE 流都正常结束 (done 事件)
    - assert: chat_message count 增量 = 4
  ```

---

## TC-MEM-L0-006 历史消息 user/assistant 交替

- **ID**: TC-MEM-L0-006
- **Type**: api
- **Priority**: P1
- **Status**: ready
- **Steps**:
  1. 发送 5 轮对话
  2. 调 Python 端接口, 查 L0 history 的 `messages` 数组
- **Expected**:
  - `messages` 数组按 `[{role:user, ...}, {role:assistant, ...}, ...]` 严格交替
  - 首位是 user, 末位是 user (新消息)
  - 总条数 = 2N (N 轮)
- **AutoCheck**:
  ```yaml
  interceptor: python_stream_generate
  field: messages
  assert:
    - length_eq: 10
    - first_role: user
    - last_role: user
    - alternating: true
  ```

---

## TC-MEM-L0-007 空历史时不报错

- **ID**: TC-MEM-L0-007
- **Type**: api
- **Priority**: P0
- **Status**: ready
- **PreConditions**:
  - session 完全空 (无任何历史)
- **Steps**:
  1. 发送消息 1: "你好"
- **Expected**:
  - HTTP 200
  - AI 正常回复 (不报 "history 为空" 错误)
  - chat_message 新增 1 条 user + 1 条 assistant
- **AutoCheck**:
  ```yaml
  steps:
    - send_message: "你好"
      expect_status: 200
      expect_reply_not_empty: true
  ```

---

## TC-MEM-L0-008 中断后续接

- **ID**: TC-MEM-L0-008
- **Type**: e2e
- **Priority**: P1
- **Status**: ready
- **Steps**:
  1. 发送消息 1: "讲个笑话"
  2. AI 回复到一半, 关闭页面 (SSE 断开)
  3. 重新进入 chat, 发送消息 2: "再讲一个"
- **Expected**:
  - 消息 1 的 assistant 回复 (即使被截断) 仍保存到 chat_message
  - 消息 2 引用消息 1 上下文 (未截断的部分)
- **AutoCheck**:
  ```yaml
  steps:
    - send_message: "讲个笑话"
    - abort_sse_after_events: 2
    - reload_page
    - send_message: "再讲一个"
      expect_reply_contains_any: ["笑话", "幽默", "开心"]
  db_assert:
    - table: chat_message
      where: { session_id: ${sessionId}, role_type: 'ASSISTANT' }
      count_min: 1
  ```

---

## TC-MEM-L0-009 多模态附件 (图片) 在历史中

- **ID**: TC-MEM-L0-009
- **Type**: e2e
- **Priority**: P1
- **Status**: ready
- **Steps**:
  1. 上传图片附件 + 文字 "这张图是什么"
  2. AI 回复
  3. 发送消息 2: "刚才那张图是什么"
- **Expected**:
  - 消息 2 的 AI 回复能引用图片内容
  - chat_message.attachments 字段存图片 base64 / URL
- **AutoCheck**:
  ```yaml
  steps:
    - upload_image: "test.png"
      with_text: "这张图是什么"
    - send_message: "刚才那张图是什么"
      expect_reply_contains: "图"   # 能回忆
  db_assert:
    - table: chat_message
      where: { session_id: ${sessionId}, attachments: not_null }
      count_min: 1
  ```

---

## TC-MEM-L0-010 关闭 L0 时不传 history

- **ID**: TC-MEM-L0-010
- **Type**: api
- **Priority**: P1
- **Status**: ready
- **PreConditions**:
  - test_user_01 的偏好 `enableLongMemory=false` (关闭 L0)
- **Steps**:
  1. 发送消息 1: "我叫 JC"
  2. 发送消息 2: "我叫什么名字"
- **Expected**:
  - 消息 2 的 AI 回复**不**包含 "JC" (L0 关闭, 无上下文)
  - Python 端 `messages` 数组**不**含历史 (只有当前 question)
- **AutoCheck**:
  ```yaml
  steps:
    - set_preference: { user_id: test_user_01, enableLongMemory: false }
    - send_message: "我叫 JC"
    - send_message: "我叫什么名字"
      expect_reply_not_contains: "JC"
  interceptor:
    field: messages
    expect_length_lte: 2   # 仅 system + current
  ```
- **Notes**: L0 与 L2 (enableMemoryProfile) 独立, 本用例只关 L0

---

## 用例统计

| 用例 | 类型 | 优先级 | 状态 |
|------|------|--------|------|
| TC-MEM-L0-001 | e2e | P0 | ready |
| TC-MEM-L0-002 | e2e | P0 | ready |
| TC-MEM-L0-003 | api | P1 | ready |
| TC-MEM-L0-004 | e2e | P0 | ready |
| TC-MEM-L0-005 | api | P0 | ready |
| TC-MEM-L0-006 | api | P1 | ready |
| TC-MEM-L0-007 | api | P0 | ready |
| TC-MEM-L0-008 | e2e | P1 | ready |
| TC-MEM-L0-009 | e2e | P1 | ready |
| TC-MEM-L0-010 | api | P1 | ready |
