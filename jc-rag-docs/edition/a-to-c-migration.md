# Python 端架构演进：A → C 迁移指南

> **目的**：记录 jc-rag Python 端从方案 A（HTTP 跨进程）切换到方案 C（git subtree 完全独立）的完整上下文、决策依据、切换步骤、自动化脚本。本文档是 AI 长期记忆的"上下文恢复源"，忘记时读这一份即可还原所有决策。

---

## 一、上下文还原（背景）

### 1.1 项目分版定位
- **开源版 (community)**：MIT/Apache License，向社区公开
- **商业版 (pro)**：私有 License，付费功能
- **目标架构**：单仓 + 未来 v1.0 时 `git subtree split` 拆出 `community` 独立公开仓

### 1.2 核心需求（用户原话）
- 商业版**不通过 docker 容器对开源版发起调用**
- 开源版作为 **jar 包** 或**代码内直接调用**
- 开源/商业代码物理隔离
- 双版本维护成本最低

### 1.3 Java 端的"天然解"
- Java 用 maven jar 依赖，pro 通过 `pom.xml` 依赖 `jc-rag-admin` 等开源 jar
- pro 在编译期就与 community jar 耦合（同进程、classloader 隔离）
- **用户诉求在 Java 端天然满足**

### 1.4 Python 端的"无解"
- Python 没有 maven 这种强依赖工具
- community Python 跑 1 个 FastAPI 进程，pro Python 跑另 1 个
- 两者只能通过 HTTP/gRPC 通信，**没有 jar 那种"直接调用"原生支持**
- 必须在 A / B / C 三方案中选一

---

## 二、决策记录

### 2.1 三方案对比

| 维度 | A: HTTP 跨进程 | B: 同进程 import | C: subtree 完全独立 |
|---|---|---|---|
| 部署 | 2 个 Python 容器 | 1 个 Python 容器 + volume 挂载 | 1 个 Python 容器（pro 自带 community 副本） |
| 调用延迟 | HTTP 100-500ms | 0ms | 0ms |
| 故障隔离 | ✅ 强 | ❌ 共享进程 | ❌ 共享进程 |
| 依赖版本 | ✅ 独立 | ❌ 冲突风险 | ✅ pro 内部统一 |
| v1.0 拆分 | ✅ 零成本 | ❌ 需重构路径 | ✅ 零成本 |
| License 风险 | ✅ 清晰 | ⚠️ 派生作品 | ⚠️ 需审许可证 |
| 商业独立性 | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 同步成本 | 无 | 高（手工 cp） | 中（subtree pull） |

### 2.2 不推荐 B 的原因（重要）

#### 坑 1：路径硬耦合
- B 方案在 `docker-compose.pro.yml` 中需要 `volumes: - ../../jc-rag-community/jc-rag-service/app:/app/community:ro`
- v1.0 拆出 community 独立仓后，路径必须改
- 仓名/路径一变，pro 全崩

#### 坑 2：依赖地狱
- 同进程只能装 1 套 `langchain/chromadb/openai` 版本
- pro 升级依赖会被 community 锁版本
- Java 有 classloader 隔离，Python 没有

#### 坑 3：故障爆炸半径
- community Python OOM/死循环 → pro 一起挂
- 无任何隔离

#### 坑 4：License 法律风险
- pro 进程包含 community 代码 → 可能构成"派生作品"
- v1.0 公开 community 时，需先做法律审查

### 2.3 当前决策（用户拍板）
- **短期**：选 **A（HTTP 跨进程）**
- **长期**：升级到 **C（git subtree 完全独立）**
- **绝不选 B**

---

## 三、方案 A 当前状态

### 3.1 架构图

```
┌─────────────────────┐       HTTP/REST       ┌─────────────────────┐
│ jc-rag-pro-service  │  ──────────────────►  │ jc-rag-service      │
│ (pro Python, 容器)  │                       │ (community Python)  │
│                     │  ◄──────────────────  │                     │
│ - advanced_search/  │                       │ - app/search/       │
│ - analytics/        │                       │ - app/embedding/    │
│ - eval/             │                       │ - app/llm/          │
└─────────────────────┘                       └─────────────────────┘
        ▲
        │ HTTP
        │
┌─────────────────────┐
│ jc-rag-admin (Java) │
│ jc-rag-user (Java)  │
└─────────────────────┘
```

### 3.2 关键路径
- community Python 容器：`jc-rag-community/jc-rag-service/`（未来改名）
- pro Python 容器：`jc-rag-pro-core/jc-rag-pro-service/`
- 配置文件：`jc-rag-community/jc-rag-service/rag_config.yml`（community URL 默认 `http://localhost:8000`）

### 3.3 启动命令
```bash
# community Python
cd jc-rag-community/jc-rag-service
python -m uvicorn main:app --port 8000

# pro Python
cd jc-rag-pro-core/jc-rag-pro-service
COMMUNITY_PYTHON_URL=http://localhost:8000 python -m uvicorn pro_main:app --port 8001
```

### 3.4 pro 调用 community 的代码示例
```python
# jc-rag-pro-core/jc-rag-pro-service/advanced_search/service.py
import httpx

COMMUNITY_PYTHON_URL = "http://localhost:8000"

async def search(query: str):
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            f"{COMMUNITY_PYTHON_URL}/api/v1/search",
            json={"query": query}
        )
        return resp.json()
```

---

## 四、方案 C 目标状态

### 4.1 架构图

```
┌──────────────────────────────────────┐
│ jc-rag-pro-service (单容器)          │
│                                      │
│  ┌─ jc-rag-core/  ← git subtree       │
│  │   ├─ app/search/                  │
│  │   ├─ app/embedding/               │
│  │   └─ app/llm/                     │
│  │                                   │
│  ├─ pro/                             │
│  │   ├─ advanced_search/  → import   │
│  │   ├─ analytics/        → import   │
│  │   └─ eval/             → import   │
│  └─ main.py                          │
└──────────────────────────────────────┘
```

### 4.2 关键路径
- pro Python 容器：`jc-rag-pro-core/jc-rag-pro-service/`
- jc-rag 副本（subtree 同步）：`jc-rag-pro-core/jc-rag-pro-service/jc-rag-core/`
- 同步脚本：`jc-rag-pro-script/sync-pro-core.sh`

### 4.3 调用代码示例
```python
# jc-rag-pro-core/jc-rag-pro-service/pro/advanced_search/service.py
from community_core.app.search.service import SearchService  # 直接 import, 0ms

class ProSearchService:
    def __init__(self):
        self._inner = SearchService()

    async def search(self, query: str):
        # 商业增强：加分、重排、过滤
        result = await self._inner.search(query)
        return self._enhance(result)
```

---

## 五、切换步骤（A → C）

### 5.1 前置条件
- [ ] community Python v1.0 已发布（API 稳定）
- [ ] pro Python 已用 A 方案跑通所有商业功能
- [ ] v1.0 时已用 `git subtree split` 拆出 community 公开仓

### 5.2 切换步骤

#### Step 1：初始化 subtree
```bash
# 假设 community 公开仓已建
COMMUNITY_REPO=https://gitee.com/Open-JC/jc-rag-community.git

# 在 pro 仓根目录
cd d:\Project\jc-rag

# 拉取 community 核心 Python 到 pro
git remote add community "$COMMUNITY_REPO"
git fetch community
git subtree add \
    --prefix=jc-rag-pro-core/jc-rag-pro-service/community_core \
    community/main --squash
```

#### Step 2：改 import 路径
- pro 内所有 `httpx.post(COMMUNITY_PYTHON_URL + '/api/v1/search')` 改为 `from community_core.app.search import SearchService`
- 搜索关键词：`COMMUNITY_PYTHON_URL`、`httpx`

#### Step 3：删除 community Python 容器
- `docker-compose.pro.yml` 删除 community Python service
- 改 pro Python 启动命令，添加 community_core 到 PYTHONPATH

#### Step 4：测试
- 单元测试：`pytest jc-rag-pro-core/jc-rag-pro-service/tests/`
- 集成测试：起 pro Python 容器，调用所有 API，验证结果与 A 方案一致

#### Step 5：提交
```bash
git add .
git commit -m "refactor(python): migrate from HTTP (A) to subtree (C) integration

- 初始化 community_core subtree 同步
- pro 改 import community_core 直接调用
- 移除 community Python 容器依赖
- 性能提升: HTTP 100ms+ → 0ms
"
```

### 5.3 后续同步流程

```bash
# community 仓每次发版时, 在 pro 仓跑:
bash jc-rag-pro-script/sync-pro-core.sh
```

脚本自动：
1. fetch community
2. subtree merge community_core
3. 输出 diff 报告
4. 提示开发者测试 + 提交

---

## 六、自动化脚本

### 6.1 `jc-rag-pro-script/sync-pro-core.sh`

```bash
#!/bin/bash
# 半自动同步 community Python 核心到 pro (subtree 模式)
# 用法: bash jc-rag-pro-script/sync-pro-core.sh

set -e

COMMUNITY_REMOTE=${COMMUNITY_REMOTE:-community}
COMMUNITY_BRANCH=${COMMUNITY_BRANCH:-main}
PRO_CORE_DIR="jc-rag-pro-core/jc-rag-pro-service/community_core"

echo "==== [1/4] Fetch community ===="
git fetch "$COMMUNITY_REMOTE" "$COMMUNITY_BRANCH"

echo "==== [2/4] Check diff ===="
if git diff --quiet "$COMMUNITY_REMOTE/$COMMUNITY_BRANCH"...HEAD -- "$PRO_CORE_DIR"; then
    echo "No diff, exit"
    exit 0
fi

CHANGED=$(git diff --name-only "$COMMUNITY_REMOTE/$COMMUNITY_BRANCH"...HEAD -- "$PRO_CORE_DIR" | wc -l)
echo "Community core has $CHANGED file changes"

echo "==== [3/4] Subtree merge ===="
git subtree merge --prefix="$PRO_CORE_DIR" \
    "$COMMUNITY_REMOTE/$COMMUNITY_BRANCH" --squash \
    -m "sync: merge community core $(date +%Y%m%d)"

echo "==== [4/4] Report ===="
echo "Sync done. Please:"
echo "  1. Review: git diff HEAD~1"
echo "  2. Test:   pytest jc-rag-pro-core/jc-rag-pro-service/tests"
echo "  3. Commit: git commit --amend"
```

### 6.2 回滚脚本（如切换失败）

```bash
# 回滚到 A 方案
git revert --no-commit HEAD
# 恢复 docker-compose.pro.yml 中 community Python service
git checkout HEAD~1 -- docker-compose.pro.yml
git commit -m "revert: rollback from C to A"
```

---

## 七、切换成本估算

| 项目 | 工作量 | 风险 |
|---|---|---|
| 初始化 subtree | 1 小时 | 低（一次操作） |
| 改 import 路径 | 4-8 小时 | 中（需逐文件改） |
| 改 docker-compose | 1 小时 | 低 |
| 测试 | 4-8 小时 | 中 |
| 文档同步 | 2 小时 | 低 |
| **总计** | **12-20 小时（1.5-2.5 工作日）** | 中 |

**建议切换时机**：
- community Python v1.0 发布后 1 个月（让 API 稳定）
- pro 商业版 v1.0 准备 GA 时（一次到位）

---

## 八、风险清单

| 风险 | 等级 | 应对 |
|---|---|---|
| community API 频繁变 | 中 | 等 v1.0 后再切 |
| subtree merge 冲突 | 中 | 脚本自动提示 + 手工解决 |
| community_core 漂移 | 低 | subtree 同步通道保证 |
| License 传染 | 低 | v1.0 时改 community 为 Apache 2.0，pro 内部仍可独立使用 |

---

## 九、决策时间线

| 日期 | 决策 | 原因 |
|---|---|---|
| 2026-06-29 | 选 A 短期方案 | 跑通流程优先 |
| 2026-06-29 | 规划 C 长期方案 | 商业独立性 |
| 待 v1.0 | 执行 A→C 切换 | community Python 稳定后 |

---

**文档结束。后续切换时，按本文档"五、切换步骤"执行即可。**
