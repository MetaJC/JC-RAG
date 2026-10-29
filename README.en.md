# JC RAG Community Edition

> **JC RAG** is an enterprise-grade RAG (Retrieval-Augmented Generation) knowledge base system, providing multi-turn dialogue, knowledge base management, access control, and intelligent retrieval.

中文文档: [README.md](README.md)

## What is this repository

This is the **open-source, free edition** of JC RAG, independently maintained. The commercial edition (JC RAG Pro) is built on top of this repository in a private repository. See the online documentation for details.

Source repository:
- Gitee: https://gitee.com/Open-JC/jc-rag
- GitHub: https://github.com/MetaJC/JC-RAG

## Repository structure

```
jc-rag-community/                              ← public repo root
├── jc-rag-frontend/                           ← frontend (Vue 3 + Vite)
│   ├── jc-rag-admin-web/                      ← admin console
│   └── jc-rag-user-web/                       ← end-user portal
│
├── jc-rag-backend/                            ← Java backend (Spring Boot 3.3)
│   ├── jc-rag-common/                         ← shared code (interfaces / DTO / exceptions)
│   ├── jc-rag-domain/                         ← domain layer (MyBatis-Plus + Mapper XML)
│   ├── jc-rag-sms/                            ← SMS service
│   ├── jc-rag-admin/                          ← admin backend (with Flyway migrations)
│   └── jc-rag-user/                           ← end-user backend
│
├── jc-rag-service/                            ← Python RAG service (FastAPI)
│   ├── app/                                   ← application code
│   ├── main.py                                ← FastAPI entry point
│   ├── rag_config.yml                         ← RAG base config
│   ├── rag_config.local.yml.example           ← local private override example
│   └── requirements.txt                       ← Python dependencies
│
├── docker-compose.yml                         ← one-click orchestration
├── LICENSE                                    ← Apache License 2.0
└── README.md
```

> Full documentation (architecture, database, API, operations, edition split/migration guides, etc.) is published as **online docs** and is not distributed with this repository.

## Quick start

### Prerequisites
- JDK 17+
- Maven 3.8+
- Node.js 20+
- Python 3.10+
- MySQL 8.0+ / Redis 7+ / Elasticsearch 8.x / Milvus 2.4+ / MinIO

### 1. Initialize the database
Create the `jc_rag` database (utf8mb4). Flyway runs migrations automatically on backend startup (under `jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/`). No manual import needed:

```sql
CREATE DATABASE jc_rag DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Start the backend
```bash
cd jc-rag-backend
mvn -pl jc-rag-common,jc-rag-domain -am clean install -DskipTests
mvn -pl jc-rag-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Start the frontend
```bash
cd jc-rag-frontend/jc-rag-admin-web
npm install
npm run dev
```

### 4. Start the Python RAG service
```bash
cd jc-rag-service
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

> For local model paths / real credentials: copy `rag_config.local.yml.example` to `rag_config.local.yml` (gitignored, not committed); it is deep-merged and overrides `rag_config.yml` at load time.

## Environment variable overrides

Database / Redis / MinIO / Elasticsearch / Python RAG addresses in the dev configs support environment variable overrides in the form `${ENV:default}`:

| Env var | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `PYTHON_RAG_URL` | `http://localhost:8001` | Python RAG service URL |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO AccessKey |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO SecretKey |
| `ELASTICSEARCH_URL` | `http://localhost:9200` | Elasticsearch URL |

> Docker Compose injects `MYSQL_ROOT_PASSWORD` / `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` via `.env` (see `.env.example`).

## Core capabilities
- ✅ Multi-turn dialogue — context management & conversation history
- ✅ Knowledge base management — upload / parse / chunk / vectorize
- ✅ Multi-format support — PDF / DOCX / PPTX / XLSX / TXT / OCR
- ✅ Vector retrieval — Milvus integration
- ✅ Full-text retrieval — Elasticsearch integration
- ✅ RBAC — users / roles / menus / data permissions
- ✅ Audit logs — operation / exception / failed-login logs
- ✅ Multi-LLM providers — OpenAI / domestic models / Ollama

## Commercial upgrades
For the following capabilities, upgrade to **JC RAG Pro**:
- 🔒 Advanced retrieval — vector + full-text + rerank fusion
- 📊 RAG evaluation — Faithfulness / Answer Relevance / Context Precision / Context Recall
- 📈 Knowledge base analytics — session stats / satisfaction / knowledge gaps
- 🛡️ Row/column-level permissions + data masking
- 🎫 License validation — RSA signature + remote verification
- 💬 7x24 technical support

Business inquiries: jc-rag@example.com

## Contributing
Issues and PRs are welcome. Before submitting, please ensure:
- Follow the Alibaba Java Coding Guidelines
- Unit test coverage ≥ 80%
- Pass all CI checks

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Security
If you discover a security vulnerability, please follow the private disclosure process in [SECURITY.md](SECURITY.md) instead of opening a public issue.

## License
Apache License 2.0 — see the [LICENSE](LICENSE) file.
