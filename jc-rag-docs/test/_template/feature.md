# 功能级测试用例模板

> 一个功能 (`FEATURE_ID`) 对应一个 `test-cases.md`, 含前置 / 多条用例 / 后置.
> 用 `## TC-...` 二级标题分隔各条用例 (见 [case.md](case.md)).

## 模板 (复制本块)

```markdown
# Feature: {FEATURE_ID} - {功能名}

- **Feature ID**: MEM-L0
- **Name**: L0 短期记忆
- **Module**: memory
- **Priority**: P0
- **CreatedAt**: 2026-07-03
- **Author**: JC
- **CodeRefs**:
  - backend: `jc-rag-backend/jc-rag-user/.../UserChatServiceImpl.java#buildShortTermHistory`
  - python: `jc-rag-service/app/api/routers/chat.py#stream_generate`
  - frontend: `jc-rag-frontend/jc-rag-user-web/src/views/user/ChatHomeView.vue`
  - api: `POST /api/chat/ask/stream`
- **ConfigRefs**:
  - `application.yml: jc-rag.memory.l0.history-rounds=10`
  - `application.yml: jc-rag.memory.l0.token-budget=4000`
- **PreConditions** (全局, 用例间共享):
  - 用户 `test_user_01` 已登录
  - jc-rag-user (port 8082) + jc-rag-service (port 8001) 启动
  - MySQL / ES / Redis 健康
  - 已有 1 个空 session (id = ${sessionId}, 见 data/users.md)
- **PostConditions** (跑完清理):
  - 删除本 feature 测试产生的 chat_message (where session_id = ${sessionId})
  - 删除本 feature 测试产生的 L1 summary (where session_id = ${sessionId})
  - 软删本 feature 测试产生的 L2 facts (where user_id = test_user_01 AND source = 'test_agent')
- **Cases**: 见下方 TC-... 块
- **TotalCases**: 10

---

## TC-MEM-L0-001 简明标题

(参考 case.md 模板, 此处省略)

---

## TC-MEM-L0-002 简明标题

(同上)

...

---

## 依赖关系 (可选)

- TC-MEM-L0-002 依赖 TC-MEM-L0-001 (后者先生成数据, 前者验证截断)
- TC-MEM-L0-010 独立 (验证开关, 不依赖其他)
```

## 与单条 case.md 的区别

| 维度 | case.md | feature.md (本文) |
|------|---------|-------------------|
| 粒度 | 1 条用例 | 1 个功能 (N 条用例) |
| 用途 | Agent 解析单条 | Agent 解析整个功能 |
| PreConditions | 每条独立 (or 引用 feature) | 全局, 用例间共享 |
| PostConditions | 单条 | 整个 feature 跑完清理 |
| 字段 | 12 个 | 11 个 (比 case 少 Expected, 多 PostConditions) |

## 命名

- 文件名: `test-cases.md` (固定, 不用 `tc.md` / `cases.md`)
- 路径: `docs/test/features/{FEATURE_ID}/test-cases.md`
- 同一 feature 跑多次的 result: `result-{YYYYMMDD-HHmm}.md` (多次并存)
