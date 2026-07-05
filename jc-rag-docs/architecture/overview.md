# jc-rag 项目总览

> 单仓双版本架构，根目录 `D:\Project\jc-rag`（v1.0 之前为单仓，未来拆 community 为独立公开仓）

---

## 一、定位

| 版本 | 名称 | License | 仓类型 |
|---|---|---|---|
| **开源版** | jc-rag | MIT（待定） | v1.0 拆为独立公开仓 |
| **商业版** | jc-rag-pro | 商业私有 License | 始终私有单仓 |

---

## 二、目录结构（v1.0 目标结构）

```
D:\Project\jc-rag\                                    (项目根, 不动)
│
├─ 📂 jc-rag-community/                                         ← 开源版 (旧 community/)
│  │
│  ├─ docker-compose.yml                              ← 开源版完整 compose
│  ├─ 📂 jc-rag-backend/                              ← Java 后端
│  │  ├─ pom.xml
│  │  ├─ jc-rag-common/                               (公共 jar)
│  │  ├─ jc-rag-admin/                                (管理端)
│  │  ├─ jc-rag-user/                                 (用户端)
│  │  ├─ jc-rag-kb-search/                            (检索)
│  │  ├─ jc-rag-kb-eval/                              (评估)
│  │  └─ jc-rag-kb-analytics/                         (分析)
│  │
│  ├─ 📂 jc-rag-service/                              ← Python 后端
│  │  ├─ app/
│  │  ├─ main.py
│  │  ├─ pyproject.toml
│  │  └─ requirements.txt
│  │
│  ├─ 📂 jc-rag-frontend/                             ← 前端
│  │  ├─ jc-rag-admin-web/
│  │  └─ jc-rag-user-web/
│  │
│  └─ 📂 jc-rag-docs/                                 ← 开源版文档
│     ├─ README.md
│     ├─ api.md
│     └─ ...
│
├─ 📂 jc-rag-pro-core/                                ← 商业版核心 (旧 pro/)
│  ├─ 📂 jc-rag-pro-backend/                          ← Java 后端
│  │  └─ jc-rag-pro/
│  ├─ 📂 jc-rag-pro-service/                          ← Python 后端
│  └─ 📂 jc-rag-pro-frontend/                         ← 前端
│
├─ 📂 jc-rag-pro-enterprise/                          ← 商业版编排 (旧 enterprise/)
│  ├─ jc-rag-pro-app-admin/
│  └─ jc-rag-pro-app-user/
│
├─ 📂 jc-rag-pro-docs/                                ← 商业版文档
├─ 📂 jc-rag-pro-script/                              ← 商业版脚本 (旧 scripts/)
│
└─ docker-compose.pro.yml                             ← 商业版完整 compose
```

---

## 三、命名规范

| 层级 | 开源版前缀 | 商业版前缀 |
|---|---|---|
| 顶层目录 | `jc-rag-community/` | `jc-rag-pro-core/` |
| Java 后端 | `jc-rag-backend/` | `jc-rag-pro-backend/` |
| Python 后端 | `jc-rag-service/` | `jc-rag-pro-service/` |
| 前端 | `jc-rag-frontend/` | `jc-rag-pro-frontend/` |
| 文档 | `jc-rag-docs/` | `jc-rag-pro-docs/` |
| 编排 | (无) | `jc-rag-pro-enterprise/` |
| 脚本 | (无) | `jc-rag-pro-script/` |
| 内部模块 | `jc-rag-admin/user/common/...` | `jc-rag-pro/...` |

**内部模块名保持不变**（jc-rag-admin、jc-rag-user、jc-rag-common 等）。

---

## 四、双版本模块关系

### 4.1 Java 端（maven jar 依赖）

```
┌──────────────────────────────┐
│ jc-rag-pro-app-admin         │  商业版 admin 启动
│ (jcrag-pro-enterprise)       │
│                              │
│  pom.xml 依赖:               │
│  ├─ jc-rag-admin.jar   ◄────┼─ maven 本地仓库 (community 编译产物)
│  └─ jc-rag-pro.jar          │
└──────────────────────────────┘
         │ 启动时加载
         ▼
┌──────────────────────────────┐
│ jc-rag-admin                 │  community 模块
│                              │
│  - 实现 ProFeatureService    │  ← 通过 @ConditionalOnProperty 切换
│  - 实现 License 校验         │
└──────────────────────────────┘
```

**关键点**：
- pro 不通过 HTTP 调用 community，**是同进程 jar 依赖**
- 商业版 vs 开源版的实现类通过 `@ConditionalOnProperty(name = "jc-rag.edition", havingValue = "pro")` 切换
- pro 启动时，classloader 同时加载 community jar 和 pro jar，pro 覆盖 community 同名 Bean

### 4.2 Python 端（当前 A 方案：HTTP 跨进程）

```
┌────────────────────────────┐         HTTP          ┌────────────────────────────┐
│ jc-rag-pro-service         │  ──────────────────►  │ jc-rag-service             │
│ (pro Python, port 8001)    │                       │ (community Python, 8000)   │
│                            │  ◄──────────────────  │                            │
│ - advanced_search/         │                       │ - app/search/              │
│ - analytics/               │                       │ - app/embedding/           │
│ - eval/                    │                       │ - app/llm/                 │
└────────────────────────────┘                       └────────────────────────────┘
```

**未来 C 方案**：见 [a-to-c-migration.md](./a-to-c-migration.md)

### 4.3 前端端（路由扩展 + 覆盖）

```
jc-rag-community/jc-rag-frontend/jc-rag-admin-web/
└─ src/router/
   ├─ index.ts                    ← 基础路由
   └─ pro-extension.ts            ← ★路由扩展点 (开源版为空数组)

jc-rag-pro-core/jc-rag-pro-frontend/src/
└─ pro-router.ts                  ← 商业版路由 (构建时合并到 pro-extension)
```

**构建流程**：`build-pro.sh` 把 pro 路由注入 community，npm build 一次输出含 pro 路由的前端 dist。

---

## 五、技术栈

| 端 | 技术 | 版本 |
|---|---|---|
| Java 后端 | Spring Boot | 3.3.5 |
| Java 后端 | MyBatis-Plus | 3.5.x |
| Python 后端 | FastAPI | 0.110+ |
| Python 后端 | LangChain | 0.1.x |
| Python 后端 | Milvus SDK | 2.4.x |
| 前端 | Vue 3 | 3.4+ |
| 前端 | Element Plus | 2.7+ |
| 前端 | Vite | 5.x |
| 基础设施 | MySQL | 8.0 |
| 基础设施 | Elasticsearch | 8.17 |
| 基础设施 | Redis | 7 |
| 基础设施 | MinIO | latest |
| 基础设施 | Milvus | 2.6.5 |
| 基础设施 | Docker Compose | v2 |

---

## 六、git 仓库策略

### 6.1 当前（v0.x）
- 单仓：`D:\Project\jc-rag`
- 1 个 git remote：`origin` → `gitee.com/Open-JC/jc-rag.git`（私有）
- 双版本代码物理隔离（jc-rag-community/ + jc-rag-pro-core/ + jc-rag-pro-enterprise/）

### 6.2 v1.0 目标（公开时）
- 拆 2 仓：
  - `jc-rag` 公开仓（gitee + github）← `git subtree split` 提取 `jc-rag-community/`
  - `jc-rag-pro` 私有仓（gitee + github）← 保留 `jc-rag-pro-core/` + `jc-rag-pro-enterprise/` + `jc-rag-pro-script/`
- 商业仓用 `git subtree add` 把 jc-rag 公开仓**拉回**作 `jc-rag-core/`（Python 端 C 方案）

### 6.3 v1.0+ 同步机制
- `jc-rag-pro-script/extract-community.sh`：单仓 → 公开仓（一次）
- `jc-rag-pro-script/sync-community.sh`：日常同步公开仓代码到商业仓
- `jc-rag-pro-script/sync-pro-core.sh`：商业仓同步 jc-rag-core（Python 端 C 方案专用）

---

## 七、版本号规范

| 类型 | 格式 | 示例 | 说明 |
|---|---|---|---|
| 开源版 | `MAJOR.MINOR.PATCH` | `0.1.0` | community 整体版本 |
| 商业版 | `MAJOR.MINOR.PATCH-pro.N` | `0.1.0-pro.1` | 商业版独立版本号 |
| 内部模块 | 与整体一致 | `0.1.0-SNAPSHOT` | maven 内部 |

**同步规则**：
- 商业版 v0.1.0-pro.1 对应 community v0.1.0
- community v0.2.0 发布后，pro 也升级为 v0.2.0-pro.1

---

## 八、相关文档

| 文档 | 说明 |
|---|---|
| [a-to-c-migration.md](./a-to-c-migration.md) | Python 端 A → C 切换指南 |
| [subtree-guide.md](./subtree-guide.md) | v1.0 subtree 拆分指南 |
| [migration-guide.md](./migration-guide.md) | v1.0 拆分完整流程 |
| [multi-platform-deploy.md](../ops/multi-platform-deploy.md) | gitee + github 双平台部署 |
| [release.md](../ops/release.md) | 发布流程（开源 + 商业） |
| [operations.md](../ops/operations.md) | 运维手册（开源 + 商业） |
| [system-architecture.md](../architecture/system-architecture.md) | 系统架构图 |
| [modules.md](../architecture/modules.md) | 模块关系 |
| [api.md](../api.md) | API 文档 |
