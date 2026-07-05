# 测试工程师使用教程

> 从"开发完一个功能"到"Agent 验证通过"的完整流程.

## 一、4 类角色,4 件事

| 角色 | 主要操作 | 用到的文档 |
|------|----------|------------|
| **开发者** | 开发完功能 → 写测试用例 | [README.md](README.md) §三, [_template/feature.md](_template/feature.md) |
| **测试 Agent** | 自动/手动执行用例 | [agent.md](agent.md) |
| **项目经理 / Reviewer** | 审查 result 文件 | [README.md](README.md) §四, result-*.md |
| **CI / DevOps** | 接入 GitLab CI / GitHub Actions | [README.md](README.md) §4.2 |

---

## 二、开发者教程 (主流程)

### Step 1: 完成功能开发

```bash
git checkout -b feature/MEM-L0
# ... 写代码 ...
git commit -m "feat(memory): 实现 L0 短期记忆"
```

### Step 2: 写测试用例

复制功能模板:

```bash
cp docs/test/_template/feature.md docs/test/features/MEM-L0/test-cases.md
```

填写内容(参考 [features/MEM-L0/test-cases.md](features/MEM-L0/test-cases.md)):

- 顶部 `Feature:` 块: 填 ID / Name / Module / CodeRefs
- 10 条 `## TC-MEM-L0-NNN` 用例, 每条 8 个必填字段
- AutoCheck YAML 可选 (Agent 跑得靠它)

### Step 3: 本地手动验证

用 Python CLI 跑一遍:

```bash
# 一次性安装 (本轮交付时 agent.py 还没实现, 先用 mock)
python jc-rag-service/scripts/test_agent.py --feature MEM-L0

# 输出:
# [测试工程师] 收到: 测试 MEM-L0
#   ┌─ TC-MEM-L0-001 ✅ pass (1234ms)
#   ├─ TC-MEM-L0-002 ✅ pass (890ms)
#   ├─ TC-MEM-L0-003 ❌ fail (567ms)
#   └─ ...
#   统计: 8 pass / 2 fail
# 详见: docs/test/features/MEM-L0/result-20260703-1530.md
```

### Step 4: 提交并推送

```bash
git add docs/test/features/MEM-L0/test-cases.md
git commit -m "test(memory): MEM-L0 完整测试用例 (10 条)"
git push origin feature/MEM-L0
```

### Step 5: 触发 CI 自动测试

push 后, GitLab CI 自动:

1. 检测变更文件, 匹配到 `docs/test/features/MEM-L0/`
2. 调 `POST /api/test/run { "feature": "MEM-L0", "trigger": "ci" }`
3. Agent 跑 10 条用例
4. 写 `result-{TIMESTAMP}.md`
5. 失败则 PR 标红, 阻塞 merge

---

## 三、手动触发 Agent (3 种方式)

### 方式 1: Agent 对话 (推荐)

在 jc-rag-user-web 聊天页, 选 "测试工程师" 智能体, 输入:

| 你说 | Agent 干啥 |
|------|------------|
| `测试 MEM-L0` | 跑 L0 全部用例 |
| `测试 MEM-L0 001` | 只跑第 1 条 |
| `测试 TC-MEM-L0-003` | 用 ID 跑第 3 条 |
| `测试模块 MEM` | 跑整个 MEM 模块 (L0/L1/L2/L3/P0-1/2/3) |
| `重跑 MEM-L0` | 重跑最近失败的 |
| `帮我测一下 L0 短期记忆` | LLM 解析 → `测试 MEM-L0` |
| `最近 MEM-L0 跑得怎样?` | 显示最近 result 摘要 |

Agent 实时输出进度, 跑完返回统计 + result 链接.

### 方式 2: Python CLI (开发期)

```bash
# 单 feature
python test_agent.py --feature MEM-L0

# 单条用例
python test_agent.py --case TC-MEM-L0-001

# 模块
python test_agent.py --module MEM

# 指定 result 路径
python test_agent.py --feature MEM-L0 --output ./my-result.md

# 调试模式 (打印每步 API 调用)
python test_agent.py --feature MEM-L0 --verbose
```

### 方式 3: REST API (CI / 第三方工具)

```bash
# 触发
curl -X POST http://localhost:8080/api/test/run \
  -H "Content-Type: application/json" \
  -d '{"feature": "MEM-L0", "trigger": "manual"}'

# 查状态
curl http://localhost:8080/api/test/status

# 查最近 result
curl http://localhost:8080/api/test/result/MEM-L0
```

---

## 四、Reviewer / PM 教程

### 看 PR 的测试状态

PR 页面会显示:
- `影响功能: MEM-L0`
- `本地 Agent: ✅ 8/10 pass`
- `CI 状态: ⏳ running`
- `result: docs/test/features/MEM-L0/result-20260703-1530.md`

### 读 result 文件

打开 `result-{TIMESTAMP}.md`:

```markdown
# Test Result: MEM-L0
- Summary: 7 pass / 2 fail / 1 blocked
...
## TC-MEM-L0-003 Token 预算截断
- **Status**: ❌ fail
- **Actual**: history.length = 5234 tokens (期望 <= 4000)
- **Recommendation**: 检查 L0 token 预算计算逻辑
```

### 决策

| 状态 | 决策 |
|------|------|
| 全 pass | ✅ 批准 merge |
| 部分 fail | ⚠️ 看 fail 是否阻塞主流程; 不阻塞可批准, 阻塞则打回 |
| blocked | ❌ Agent 环境问题, 重跑或人工 |

---

## 五、DevOps 教程 (CI 集成)

### GitLab CI 示例 (`.gitlab-ci.yml`)

```yaml
stages:
  - test

agent-test:
  stage: test
  image: python:3.11
  before_script:
    - pip install -r jc-rag-service/requirements-test.txt
  script:
    # 1. 检测受影响的 feature
    - python scripts/detect_changed_features.py > changed_features.txt
    # 2. 启动 jc-rag-user / jc-rag-service (用 docker compose)
    - docker compose up -d jc-rag-user jc-rag-service
    - sleep 30
    # 3. 跑 Agent
    - |
      while read feature; do
        python jc-rag-service/scripts/test_agent.py --feature "$feature" --auto || exit 1
      done < changed_features.txt
  artifacts:
    paths:
      - docs/test/features/*/result-*.md
    reports:
      junit: test-results.xml
  only:
    - merge_requests
    - main
```

### detect_changed_features.py 思路

```python
import subprocess
changed = subprocess.check_output(['git', 'diff', '--name-only', 'origin/main...HEAD'], text=True)
features = set()
for path in changed.splitlines():
    if path.startswith('jc-rag-backend/') or path.startswith('jc-rag-frontend/'):
        # 从 git log 找关联的 FEATURE_ID (commit msg 约定 "feat(MEM-L0): ...")
        commit = subprocess.check_output(['git', 'log', '--oneline', '--', path], text=True)
        for line in commit.splitlines():
            if 'MEM-L0' in line: features.add('MEM-L0')
            if 'AUTH-LOGIN' in line: features.add('AUTH-LOGIN')
print('\n'.join(features))
```

---

## 六、常见问题 (FAQ)

### Q1: 用例写完后多久生效?
CI 钩子接 push 事件, push 后 5 分钟内自动跑. 手动用 Agent 对话即时生效.

### Q2: 测试失败是开发问题还是用例问题?
看 `result-*.md` 的 `Recommendation` 字段. fail 可能是代码 bug, 也可能是用例期望太严格.

### Q3: 一次跑多个 feature 怎么写?
暂无, 需逐 feature 跑. 或用 `--module MEM` 跑整个模块.

### Q4: Agent 跑挂了 / 假死?
看 `result-*.md` 的 `StackTrace` 字段, 或 Agent 日志 (`/var/log/jc-rag/test-agent.log`, 待配).

### Q5: 用例数和功能数怎么对齐?
`find docs/test/features -name 'test-cases.md' | wc -l` 应 = 已完成功能数 (每周 review 一次).

### Q6: L0 / L1 / L2 / L3 都能测吗?
能, 每个 feature 都有独立的 test-cases.md. 但 L1/L2/L3 涉及异步任务, 测试需要等待 (建议 timeout 30s+).

---

## 七、当前可用的部分 (本轮交付)

| 模块 | 状态 |
|------|------|
| 文档骨架 (README/agent/templates) | ✅ 已交付 |
| MEM-L0 示例 (10 条用例) | ✅ 已交付 |
| 测试账号 (data/users.md) | ✅ 已交付 |
| Agent 工具实现 (8 个 tool) | ⏳ 待实现 |
| Python CLI (test_agent.py) | ⏳ 待实现 |
| REST API (/api/test/run) | ⏳ 待实现 |
| Agent 对话入口 (jcr-user-web 智能体) | ⏳ 待实现 |
| GitLab CI 钩子 | ⏳ 待实现 |

> 本轮交付: **协议 + 模板 + 示例**, 用于指导后续 Agent 实现 + 各功能用例补全.

## 八、行动清单 (按优先级)

### P0 (本周)

- [ ] 实现 Python CLI `test_agent.py` (含 8 个 tool 基础版)
- [ ] 实现 REST API `POST /api/test/run`
- [ ] 在 jcr-user-web 加 "测试工程师" 智能体入口

### P1 (下周)

- [ ] GitLab CI 接入 (示例仓库)
- [ ] 各模块用例补全: MEM-L1/L2/L3/P0-1/2/3, AUTH-LOGIN/REGISTER, KB-UPLOAD, CHAT-SEND
- [ ] 全局冒烟 0001-smoke.md (10 条)

### P2 (未来)

- [ ] 并行执行 (多个 feature 同时跑)
- [ ] 历史 result 对比 (diff 回归)
- [ ] Agent 学习 (从历史 fail 总结代码 bug 模式)
