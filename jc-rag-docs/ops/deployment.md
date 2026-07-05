# 部署手册

> 部署目标：单机 Windows / Linux 一键拉起 jc-rag 全套基础中间件和应用服务。

---

## 1. 部署架构

| 层级 | 组件 | 端口 | 启动方式 |
|---|---|---|---|
| 基础设施 | MySQL 8.0 | 3308 | docker-compose |
| 基础设施 | Elasticsearch 8.17 | 9200/9300 | docker-compose |
| 基础设施 | Redis 7 | 6379 | docker-compose |
| 基础设施 | MinIO | 9000/9001 | docker-compose |
| 基础设施 | Milvus 2.6.5 (etcd + standalone) | 19530/9091 | docker-compose |
| 模型运行时 | Ollama | 11434 | 宿主机二进制 |
| 应用 | Python RAG (FastAPI) | 8001 | 宿主机 Python venv |
| 应用 | 管理端后端 jc-rag-admin | 8080 | 宿主机 Maven |
| 应用 | C 端后端 jc-rag-user | 8082 | 宿主机 Maven |
| 应用 | 管理端前端 jc-rag-admin-web | 5173 | 宿主机 npm |
| 应用 | C 端前端 jc-rag-user-web | 5175 | 宿主机 npm |

---

## 2. 部署前置条件

### 2.1 硬件
- CPU：4 核及以上
- 内存：16 GB 及以上（Milvus 独立 4 GB，ES 独立 2 GB）
- 磁盘：100 GB 及以上可用空间（向量索引 + 文档 + 模型）

### 2.2 软件
- Docker Desktop / Docker Engine 24+
- Docker Compose v2
- JDK 17
- Maven 3.9+
- Python 3.10 / 3.11
- Node.js 20+
- Ollama 0.5+（[https://ollama.com](https://ollama.com)）

### 2.3 模型准备
```powershell
# 拉取 LLM
ollama pull qwen2.5:7b

# 拉取 Embedding（bge-large-zh-v1.5）
ollama pull bge-large-zh-v1.5
```

---

## 3. 启动顺序

> 严格按顺序启动：基础设施 → 模型 → Python RAG → Java 后端 → 前端。

### 3.1 启动基础设施（docker-compose）

```powershell
cd D:\Project\jc-rag
docker compose up -d
```

**校验**：
```powershell
docker compose ps
# 期望：mysql / elasticsearch / redis / minio / etcd / standalone 全部 healthy 或 running
```

### 3.2 初始化数据库与 ES 索引

```powershell
# MySQL 初始化（首次部署）
Get-Content docs\sql\mysql\001_jc_rag_all_in_one.sql | docker exec -i rag-mysql mysql -uroot -proot123456

# Elasticsearch 索引初始化
# 用 VS Code REST Client 打开 docs/es/001_jc_rag_all_in_one.http，逐条执行
```

### 3.3 启动 Ollama

```powershell
# 已配置为系统服务时直接启动；否则
ollama serve
# → http://localhost:11434
```

**校验**：
```powershell
curl http://localhost:11434/api/tags
# 期望返回已拉取的模型列表
```

### 3.4 启动 Python RAG 服务

```powershell
cd D:\Project\jc-rag\backend-python
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
python main.py
# → http://localhost:8001/docs
```

**校验**：
```powershell
curl http://localhost:8001/api/v1/health
# 期望：{"status":"ok"}
```

### 3.5 启动 Java 后端

```powershell
# 管理端（正式入口）
cd D:\Project\jc-rag\server
mvn -pl jc-rag-admin -am spring-boot:run
# → http://localhost:8080

# C 端（新终端）
cd D:\Project\jc-rag\server
mvn -pl jc-rag-user -am spring-boot:run
# → http://localhost:8082
```

**校验**：
```powershell
curl http://localhost:8080/api/sys/health
curl http://localhost:8082/api/health
```

### 3.6 启动前端

```powershell
# 管理端
cd D:\Project\jc-rag\frontend\jc-rag-admin-web
npm install
npm run dev
# → http://localhost:5173

# C 端（新终端）
cd D:\Project\jc-rag\frontend\jc-rag-user-web
npm install
npm run dev
# → http://localhost:5175
```

---

## 4. 健康检查清单

| 检查项 | 命令 / 路径 | 期望 |
|---|---|---|
| MySQL | `docker exec rag-mysql mysqladmin ping -uroot -proot123456` | `mysqld is alive` |
| Elasticsearch | `curl http://localhost:9200/_cluster/health` | `status: yellow`（单节点） |
| Redis | `docker exec rag-redis redis-cli ping` | `PONG` |
| MinIO | `curl http://localhost:9000/minio/health/live` | HTTP 200 |
| Milvus | `curl http://localhost:9091/healthz` | HTTP 200 |
| Ollama | `curl http://localhost:11434/api/tags` | JSON 列表 |
| Python RAG | `curl http://localhost:8001/api/v1/health` | `{"status":"ok"}` |
| 管理端后端 | `curl http://localhost:8080/api/sys/health` | JSON ok |
| C 端后端 | `curl http://localhost:8082/api/health` | JSON ok |
| 管理端前端 | 浏览器访问 `http://localhost:5173` | 登录页 |
| C 端前端 | 浏览器访问 `http://localhost:5175` | 首页 |

---

## 5. 关闭顺序

```powershell
# 前端
Ctrl+C（每个前端窗口）

# Java 后端
Ctrl+C（每个 mvn 窗口）

# Python RAG
Ctrl+C

# Ollama
Ctrl+C 或 Stop-Service ollama

# 基础设施
cd D:\Project\jc-rag
docker compose down          # 保留数据
docker compose down -v       # 同时删除数据卷（慎用）
```

---

## 6. 常见问题

### 6.1 ES 启动后 status: red
- 单节点部署默认 `yellow`；`red` 表示数据损坏或磁盘满
- 检查 `es_data` 卷磁盘空间

### 6.2 Milvus standalone 健康检查超时
- 首次启动依赖 etcd 与 minio，需等待 1~3 分钟
- 查看日志：`docker logs rag-milvus`

### 6.3 Python RAG 启动报 `pydantic` 版本冲突
- 务必使用项目自带 `requirements.txt` 锁定的版本
- 不要混用全局 Python 环境

### 6.4 Java 后端启动报端口占用
- 检查 8080/8082 是否被占用：`netstat -ano | findstr :8080`
- 修改 `application.yml` 的 `server.port` 后重启

### 6.5 前端 npm install 失败
- 切换国内镜像：`npm config set registry https://registry.npmmirror.com`
- 删除 `node_modules` 与 `package-lock.json` 后重试

---

## 7. 生产环境建议

- 所有中间件使用独立服务器或 K8s Pod，避免与 Java/Python 应用混部
- Ollama 建议独立部署，AI 推理独占 GPU
- MySQL 开启 binlog + 每日全量备份
- ES 与 Milvus 数据卷启用快照备份
- 前端使用 Nginx 反向代理 + HTTPS
- 关闭外网，仅保留内网访问
