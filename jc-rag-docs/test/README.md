# 测试用例库 (Test Engineer Agent 入口)

> 测试用例库 + 测试工程师 Agent 的统一入口. 供"开发完一个功能后写用例, 然后自动/手动触发 Agent 验证"使用.

## 一、工作流 (4 步)

```
[开发完功能]
    ↓
[1. 写测试用例] → docs/test/features/{FEATURE_ID}/test-cases.md   (开发者)
    ↓ git push
[2. CI 钩子触发] → POST /api/test/run?feature={FEATURE_ID}        (CI / GitLab CI)
    ↓
[3. Agent 自动跑] → 调 AutoCheck / 模拟操作 / 验证 Expected
    ↓
[4. 写结果] → docs/test/features/{FEATURE_ID}/result-{YYYYMMDD-HHmm}.md
```

**手动触发** (3 种, 等效):
- **Agent 对话**: 在 jc-rag-user-web "测试工程师" 智能体中输入 `测试 MEM-L0` 或 `测试 TC-MEM-L0-001`
- **CLI**: `python jc-rag-service/scripts/test_agent.py --feature MEM-L0`
- **REST**: `POST /api/test/run` `{ "feature": "MEM-L0", "caseId": "TC-MEM-L0-001" }`

## 二、目录结构

```
docs/test/
├── README.md                   ← 本文件 (入口/规约)
├── agent.md                    ← 测试工程师 Agent 角色定义 (tool/协议)
├── _template/
│   ├── case.md                 ← 单条用例模板 (Agent 解析用)
│   └── feature.md              ← 功能级用例集模板 (开发者用)
├── features/                   ← 按功能组织 (跟开发 1:1 对应)
│   ├── AUTH-LOGIN/
│   │   ├── test-cases.md       (开发者写)
│   │   └── result-20260703-1530.md  (Agent 写)
│   ├── MEM-L0/                 (示例: L0 短期记忆)
│   │   ├── test-cases.md
│   │   └── result-*.md
│   ├── MEM-L1/
│   ├── MEM-L2/
│   ├── MEM-L3/
│   ├── MEM-P01-FORGET/         (P0-1 GDPR)
│   ├── CHAT-SEND/              (聊天)
│   ├── KB-UPLOAD/              (知识库)
│   ├── ...
└── data/                       ← 测试账号 / 测试数据
    ├── users.md
    └── knowledge-bases.md
```

## 三、命名规约

### 3.1 功能 ID (`FEATURE_ID`)

格式: `{MODULE}-{NAME}`, 大写连字符, 模块缩写参考下表.

| 模块 | 缩写 | 说明 |
|------|------|------|
| 认证授权 | AUTH | 登录/注册/找回/滑块 |
| RBAC | RBAC | 用户/角色/权限/菜单/数据权限 |
| 知识库 | KB | 增删改查/导入/解析/检索/授权 |
| 聊天 | CHAT | 会话/消息/SSE 流式/多模态 |
| 记忆 | MEM | L0/L1/L2/L3 + P0-1/2/3 |
| 偏好 | PREF | 5 项 OK + 记忆文件 + 被遗忘权 |
| 管理后台 | ADMIN | 仪表盘/审计/日志 |
| 商业版 | PRO | KB 商业增强/高级搜索/评测 |
| 跨切面 | CROSS | 性能/安全/合规/SEO |

示例: `MEM-L0`, `AUTH-LOGIN`, `KB-UPLOAD`, `MEM-P01-FORGET` (P0-1 一键忘记我)

### 3.2 用例 ID

格式: `TC-{FEATURE_ID}-{NNN}` (NNN 三位数字, 同 feature 内从 001 起).

示例: `TC-MEM-L0-001`, `TC-AUTH-LOGIN-003`, `TC-MEM-P01-FORGET-002`

## 四、与开发同步

### 4.1 PR 模板 (`.gitlab/merge_request_templates/feature.md`)

```markdown
## 改动说明
(简述)

## 关联测试用例
- 新增功能 ID: (例如: MEM-L0)
- 关联用例集: docs/test/features/MEM-L0/test-cases.md

## 自测
- [ ] 本地 Agent 跑过 (附 result-*.md 链接)
- [ ] 全局冒烟未回归
```

### 4.2 CI 钩子 (伪代码)

```yaml
# .gitlab-ci.yml
on-push:
  stage: test
  script:
    - python scripts/detect_changed_features.py    # diff vs main, 列出受影响的 FEATURE_ID
    - python scripts/test_agent.py --feature $FEATURE_ID --auto
  artifacts:
    paths:
      - docs/test/features/*/result-*.md
```

### 4.3 手动同步

- 每周 review 一次: `find docs/test/features -name 'test-cases.md' | wc -l` 应 = 已完成功能数
- 每月 review: 全局冒烟 (`0001-smoke.md` 待写) PASS 率 >= 95%

## 五、测试粒度支持 (4 档)

| 粒度 | 调用 | 用途 | 耗时 |
|------|------|------|------|
| 全局 | `--feature GLOBAL-SMOKE` 或对话 "跑冒烟" | CI 每次 push | 10 min |
| 模块 | `--feature MEM` (拉所有 MEM-*) | 大版本发布前 | 1-2 h |
| 功能 | `--feature MEM-L0` | PR 必跑 | 10-30 min |
| 单条 | `--case TC-MEM-L0-001` | 开发期调试 | 1-5 min |

## 六、当前状态

| 功能 ID | 状态 | 用例数 | result 数 | 备注 |
|---------|------|--------|-----------|------|
| MEM-L0 | ⏳ 待写 | 0 | 0 | P0, 优先 |
| MEM-L1 | ⏳ 待写 | 0 | 0 | P0 |
| MEM-L2 | ⏳ 待写 | 0 | 0 | P0 |
| MEM-L3 | ⏳ 待写 | 0 | 0 | P1 |
| MEM-P01-FORGET | ⏳ 待写 | 0 | 0 | P0 (合规) |
| MEM-P02-RETRACT | ⏳ 待写 | 0 | 0 | P1 |
| MEM-P03-RETENTION | ⏳ 待写 | 0 | 0 | P1 |
| AUTH-LOGIN | ⏳ 待写 | 0 | 0 | P0 |
| AUTH-REGISTER | ⏳ 待写 | 0 | 0 | P0 |
| KB-UPLOAD | ⏳ 待写 | 0 | 0 | P0 |
| CHAT-SEND | ⏳ 待写 | 0 | 0 | P0 |
| ... | ... | ... | ... | 逐功能补 |

## 七、下一步 (本轮交付后)

1. 写 [agent.md](agent.md) (Agent 角色 + 工具集 + 协议)
2. 写 [_template/case.md](_template/case.md) (单条用例模板)
3. 写 [_template/feature.md](_template/feature.md) (功能级模板)
4. 写 [features/MEM-L0/test-cases.md](features/MEM-L0/test-cases.md) (示例, 8-10 条用例, 供 Agent 调试协议)
5. 实现 Agent 工具层 (Python): `test_agent.py` + REST `/api/test/run`
6. 接入 GitLab CI
