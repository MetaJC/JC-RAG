# JC RAG 开源版 (Community Edition)

> **JC RAG** 是一个企业级 RAG (检索增强生成) 知识库系统, 支持多轮对话、知识库管理、权限控制、智能检索等核心能力.

## 这是什么仓库

本仓库是 JC RAG 的**开源免费版本**, 独立维护. 商业付费版本 (JC RAG Pro) 在私有仓库中基于本仓库二次构建, 详情见 [docs/architecture/monorepo-edition.md](docs/architecture/monorepo-edition.md).

## 仓库结构

```
jc-rag-community/                              ← 公开仓库根
├── frontend/                                  ← 前端 (Vue 3 + Vite)
│   ├── jc-rag-admin-web/                      ← 管理后台
│   └── jc-rag-user-web/                       ← C 端用户
│
├── backend-java/                              ← Java 后端 (Spring Boot 3.3)
│   ├── jc-rag-common/                         ← 公共代码 (接口 / DTO / 异常)
│   ├── jc-rag-domain/                         ← 领域层 (MyBatis-Plus + Mapper XML)
│   ├── jc-rag-admin/                          ← 管理端后端
│   └── jc-rag-user/                           ← C 端后端
│
├── backend-python/                            ← Python RAG 服务 (FastAPI)
│   ├── app/                                   ← 应用代码
│   │   ├── agents/                            ← Agent 框架
│   │   ├── api/                               ← API 路由
│   │   ├── core/                              ← 核心 (配置 / 设备检测 / 进度)
│   │   ├── embeddings/                        ← Embedding 接入
│   │   ├── llm/                               ← LLM Provider 工厂
│   │   ├── loaders/                           ← 文档加载器 (PDF/DOCX/PPTX/XLSX/TXT)
│   │   ├── retrievers/                        ← 检索器
│   │   ├── skills/                            ← Skills (业务能力)
│   │   └── utils/                             ← 工具 (chunk / ES / MinIO / OCR)
│   ├── main.py                                ← FastAPI 入口
│   ├── rag_config.yml                         ← RAG 配置
│   └── requirements.txt                       ← Python 依赖
│
└── docs/                                      ← 文档
    ├── architecture/                          ← 架构文档
    │   ├── system-architecture.md             ← 系统架构
    │   ├── modules.md                         ← 模块说明
    │   ├── knowledge-parse-pipeline.md        ← 知识解析流水线
    │   └── monorepo-edition.md                ← 双版本架构 (开源 + 商业)
    ├── api.md                                 ← API 文档
    ├── startup-guide.md                       ← 启动指南
    ├── sql/                                   ← 数据库脚本
    │   └── mysql/                             ← MySQL 初始化脚本
    ├── es/                                    ← Elasticsearch 索引 / 查询
    └── ops/                                   ← 运维文档
```

## 快速开始

### 前置依赖

- JDK 17+
- Maven 3.8+
- Node.js 20+
- Python 3.10+
- MySQL 8.0+ / Redis 7+ / Elasticsearch 8.x / Milvus 2.4+ / MinIO

### 1. 初始化数据库

```bash
mysql -uroot -p < docs/sql/mysql/001_jc_rag_all_in_one.sql
mysql -uroot -p jc_rag < docs/sql/mysql/002_modules.sql
mysql -uroot -p jc_rag < docs/sql/mysql/003_seed_data.sql
# ... 按编号顺序执行
```

### 2. 启动后端

```bash
cd backend-java
mvn -pl jc-rag-common,jc-rag-domain -am clean install -DskipTests
mvn -pl jc-rag-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 启动前端

```bash
cd frontend/jc-rag-admin-web
npm install
npm run dev
```

### 4. 启动 Python RAG

```bash
cd backend-python
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

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

## 贡献

欢迎提交 Issue 和 PR. 提交前请确保:

- 遵循阿里 Java 开发手册
- 单元测试覆盖率 ≥ 80%
- 通过所有 CI 检查

## License

MIT License — 详见 [LICENSE](LICENSE) 文件.

## 双版本架构

本仓库是**双版本架构**中的开源仓库, 商业版通过 submodule 引用本仓库. 详细架构说明见 [docs/architecture/monorepo-edition.md](docs/architecture/monorepo-edition.md).
