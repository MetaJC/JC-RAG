# JC RAG 系统 — 启动与维护手册

## 系统架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                  外部依赖（Docker）                          │
│  MySQL:3308 │ ES:9200 │ Redis:6379 │ MinIO:9000            │
└──────────┬─────────────────────────────────────────┬────────┘
           │                                         │
┌──────────┴──────────────┐     ┌────────────────────┴────────┐
│  Python RAG 引擎        │     │  Ollama 本地大模型          │
│  FastAPI · port 8001    │     │  port 11434                 │
│  文档解析/向量嵌入/RAG   │     │  qwen2.5:1.5b 等           │
└─────────────────────────┘     └─────────────────────────────┘
           │
┌──────────┴─────────────────────────────────────────────┐
│              管理端后端（正式入口）                      │
│  jc-rag-admin · port 8080                       │
│  已吸收旧管理功能与新 admin 功能                         │
│  管理 API: /api/system /api/rbac /api/admin /api/chat   │
└────────────────────────────────────────────────────────┘
           │
┌──────────┴─────────────────────────────────────────────┐
│              C 端后端                                   │
│  jc-rag-user · port 8082                         │
│  用户 API: /api/auth /api/user /api/chat                │
│  特点：会话隔离、默认知识库、个人会话管理               │
└────────────────────────────────────────────────────────┘
           │
┌──────────┴────────────────────┐
│          前端                  │
├──────────┬─────────┤
│ 管理端    │ C端用户  │
│ 5173     │ 5175    │
│ jc-rag-admin-web │ jc-rag-user-web │
└──────────┴─────────┘
```

---

## 完整启动步骤

### 1. 启动基础设施（Docker）

```powershell
cd D:\jc-rag
docker compose up -d
```

首次需初始化全部数据库脚本（按顺序执行）：

```powershell
# 核心表
docker exec -i rag-mysql mysql -uroot -proot123456 jc_rag < docs\sql\mysql\001_jc_rag_all_in_one.sql

# 新功能模块表
docker exec -i rag-mysql mysql -uroot -proot123456 jc_rag < docs\sql\mysql\002_modules.sql

# 种子数据（用户、角色、权限、提示词等）
docker exec -i rag-mysql mysql -uroot -proot123456 jc_rag < docs\sql\mysql\003_seed_data.sql

# 角色表新增 is_admin 字段 (管理端登录准入控制)
docker exec -i rag-mysql mysql -uroot -proot123456 jc_rag < docs\sql\mysql\021_rbac_role_is_admin.sql
```

### 2. 启动 Ollama

```powershell
ollama serve
ollama pull qwen2.5:1.5b    # 推荐模型
```

### 3. 启动 Python RAG 引擎

```powershell
cd D:\jc-rag\rag-service
.venv\Scripts\activate
python main.py
# → http://localhost:8001/docs
```

### 4. 启动管理端后端

```powershell
cd D:\Project\jc-rag\server
mvn -pl jc-rag-admin -am spring-boot:run
# → http://localhost:8080
```

### 5. 启动 C 端后端

```powershell
cd D:\jc-rag\server
mvn -pl jc-rag-user -am spring-boot:run
# → http://localhost:8082
```

### 6. 启动前端

```powershell
# 管理端（新终端）
cd D:\Project\jc-rag\frontend\jc-rag-admin-web
npm run dev
# → http://localhost:5173

# C 端用户门户（新终端）
cd D:\Project\jc-rag\frontend\jc-rag-user-web
npm run dev
# → http://localhost:5175
```

---

## 一键快速启动

> 当前无统一启动脚本，请按上述 1~6 步分别启动各服务。

---

## 默认账号

| 用户名 | 密码 | 类型 | 说明 |
|--------|------|------|------|
| `developer` | `developer@2024` | DEVELOPER | 兜底账号，管理端保底可用 |
| `admin` | `123456` | ADMIN | 管理员账号 |
| `user01` | `123456` | NORMAL | 普通用户（分析员角色） |
| `user02` | `123456` | NORMAL | 普通用户 |

### 账号使用说明
- 管理端默认使用 `admin / 123456`
- C 端测试用户默认使用 `user01 / 123456`
- `developer` 保留为兜底账号，建议仅开发调试时使用

---

## 端口总览

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3308 | | 
| Elasticsearch | 9200 | |
| Redis | 6379 | |
| MinIO | 9000/9001 | |
| Python RAG | 8001 | Swagger: `/docs` |
| 管理端 Java 后端 | 8080 | 唯一正式管理端 |
| **C 端用户模块** | **8082** | 新多模块 |
| Ollama | 11434 | |
| 管理端前端 | 5173 | `frontend/jc-rag-admin-web` |
| **C 端前端** | **5175** | **新用户门户** |

---

## 功能入口

| 功能 | URL | 所属模块 |
|------|-----|---------|
| 管理端系统设置 | http://localhost:5173/system/settings | management |
| 管理端知识库 | http://localhost:5173/knowledge/base | management |
| 管理端问答中心 | http://localhost:5173/qa/center | management |
| 管理端用户管理 | http://localhost:5173/rbac/users | management |
| 管理端角色管理 | http://localhost:5173/rbac/roles | management |
| 管理端菜单管理 | http://localhost:5173/rbac/menus | management |
| 管理端权限管理 | http://localhost:5173/rbac/permissions | management |
| 管理端提示词管理 | http://localhost:5173/prompts/manage | management |
| 管理端 Agent & Skill | http://localhost:5173/agents/manage | management |
| 管理端数据权限 | http://localhost:5173/data/permissions | management |
| C 端问答首页 | http://localhost:5175/ | user |
| C 端个人中心 | http://localhost:5175/profile | user |
| C 端 API Key 管理 | http://localhost:5175/api-keys | user |
| C 端 Agent 列表 | http://localhost:5175/agents | user |

---

## 本次改造说明

- 管理端已确定为 `jc-rag-admin + frontend/jc-rag-admin-web`
- 管理端与 C 端当前都已接入 MyBatis Plus 依赖
- C 端会话已按登录用户隔离，并支持删除自己的会话
- C 端知识库已收口为“管理员设置的默认知识库”
- 默认知识库的数据库配置见 `docs/sql/mysql/004_management_merge_increment.sql`
