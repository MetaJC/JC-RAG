# JC RAG 开源版 (Community Edition)

> **JC RAG** 是一个企业级 RAG (检索增强生成) 知识库系统, 支持多轮对话、知识库管理、权限控制、智能检索等核心能力.

English: [README.en.md](README.en.md)

## 这是什么仓库

本仓库是 JC RAG 的**开源免费版本**, 独立维护. 商业付费版本 (JC RAG Pro) 在私有仓库中基于本仓库二次构建, 详见在线文档.

开源仓库地址:
- Gitee: https://gitee.com/Open-JC/jc-rag
- GitHub: https://github.com/MetaJC/JC-RAG

## 文档与在线体验

- 开发文档：https://www.openjc.cc/doc/projects/jc-rag
- 在线体验：
  - 客户端：http://rag.openjc.cc/
  - 管理端：http://admin.rag.openjc.cc/

## 仓库结构

```
jc-rag/                                    ← 公开仓库根
├── jc-rag-frontend/                           ← 前端 (Vue 3 + Vite)
│   ├── jc-rag-admin-web/                      ← 管理后台
│   └── jc-rag-user-web/                       ← C 端用户
│
├── jc-rag-backend/                            ← Java 后端 (Spring Boot 3.3)
│   ├── jc-rag-common/                         ← 公共代码 (接口 / DTO / 异常)
│   ├── jc-rag-domain/                         ← 领域层 (MyBatis-Plus + Mapper XML)
│   ├── jc-rag-sms/                            ← 短信服务
│   ├── jc-rag-admin/                          ← 管理端后端 (含 Flyway 迁移脚本)
│   └── jc-rag-user/                           ← C 端后端
│
├── jc-rag-service/                            ← Python RAG 服务 (FastAPI)
│   ├── app/                                   ← 应用代码
│   ├── main.py                                ← FastAPI 入口
│   ├── rag_config.yml                         ← RAG 基础配置
│   ├── rag_config.local.yml.example           ← 本地私有覆盖示例 (路径/凭证)
│   └── requirements.txt                       ← Python 依赖
│
├── docker-compose.yml                         ← 一键编排 (基础服务 + community Python)
├── LICENSE                                    ← Apache License 2.0
└── README.md
```

> 完整文档（架构、数据库、API、运维、版本拆分/迁移指南等）以**在线文档**形式发布, 不随仓库分发.

## 快速开始

### 前置依赖

- JDK 17+
- Maven 3.8+
- Node.js 20+
- Python 3.10+
- MySQL 8.0+ / Redis 7+ / Elasticsearch 8.x / Milvus 2.4+ / MinIO

> **Python 依赖与 PyTorch（重要）**：
> - `jc-rag-service` 的**基础依赖**（`pip install -r requirements.txt` 或 `pip install -e .`）**不含 PyTorch**。
> - **仅当使用本地模型推理（本地 Embedding / 本地 LLM）时**才需要 PyTorch：额外安装 `pip install -e .[local-model]`（会经由 `sentence-transformers` 引入 PyTorch，约 1~2GB），并按显卡选装对应 torch wheel（NVIDIA CUDA / Intel·AMD DirectML / Apple MPS / AMD ROCm / Intel XPU）。
> - **使用在线模型（OpenAI / DeepSeek / 通义千问 等，配置 `embedding.source: service`）时，不需要 PyTorch**，只装基础依赖即可。

### 1. 初始化数据库

创建数据库 `jc_rag` (utf8mb4) 即可, 后端启动时 **Flyway 会自动执行**迁移脚本 (位于 `jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/`, V1 全量建表 + V2+ 增量), 无需手动导入:

```sql
CREATE DATABASE jc_rag DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 启动后端

```bash
cd jc-rag-backend
mvn -pl jc-rag-common,jc-rag-domain -am clean install -DskipTests
mvn -pl jc-rag-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 启动前端

```bash
cd jc-rag-frontend/jc-rag-admin-web
npm install
npm run dev
```

### 4. 启动 Python RAG

```bash
cd jc-rag-service
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

> 本机模型路径 / 真实凭证等配置: 复制 `rag_config.local.yml.example` 为 `rag_config.local.yml` 进行覆盖 (已被 .gitignore 忽略, 不会提交), 加载时自动合并、优先于 `rag_config.yml`.

## 环境变量覆盖

开发配置文件中的数据库 / Redis / MinIO / Elasticsearch / Python RAG 地址均支持环境变量覆盖, 格式为 `${ENV:默认值}` (有环境变量用环境变量, 无则用默认值):

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | `root` | MySQL 密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `PYTHON_RAG_URL` | `http://localhost:8001` | Python RAG 服务地址 |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO 地址 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO AccessKey |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO SecretKey |
| `ELASTICSEARCH_URL` | `http://localhost:9200` | Elasticsearch 地址 |

> Docker Compose 通过 `.env` (参考 `.env.example`) 注入 `MYSQL_ROOT_PASSWORD` / `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` 等.

## 核心能力

- ✅ **多轮对话** — 上下文管理 + 会话历史
- ✅ **知识库管理** — 文档上传 / 解析 / 分块 / 向量化
- ✅ **多格式支持** — PDF / DOCX / PPTX / XLSX / TXT / OCR
- ✅ **向量检索** — Milvus 集成
- ✅ **全文检索** — Elasticsearch 集成
- ✅ **RBAC 权限** — 用户 / 角色 / 菜单 / 数据权限
- ✅ **审计日志** — 操作日志 / 异常日志 / 登录失败
- ✅ **LLM 多 Provider** — OpenAI / 国产模型 / Ollama

## 商业版增强

需要以下能力, 请升级到 **JC RAG Pro** 商业版:

- 🔒 **高级检索** — 向量 + 全文 + 重排序融合
- 📊 **RAG 评测** — Faithfulness / Answer Relevance / Context Precision / Context Recall
- 📈 **知识库分析** — 会话统计 / 满意度 / 知识缺口
- 🛡️ **行/列级权限 + 数据脱敏**
- 🎫 **License 校验** — RSA 签名 + 远程校验
- 💬 **7x24 技术支持**

商业合作: jc-rag@example.com

> 产品线规划为社区版 / 专业版 (Pro) / 企业版 (Enterprise) 三个版本, 商业合作请邮件联系.

## 贡献

欢迎提交 Issue 和 PR. 提交前请确保:

- 遵循阿里 Java 开发手册
- 单元测试覆盖率 ≥ 80%
- 通过所有 CI 检查

详见 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## 安全

如发现安全漏洞, 请按 [SECURITY.md](SECURITY.md) 的流程私披露, 勿公开 Issue.

## License

Apache License 2.0 — 详见 [LICENSE](LICENSE) 文件.
