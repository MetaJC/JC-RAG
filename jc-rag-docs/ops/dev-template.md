# Dev 模板维护流程（application*.yml 治理）

> 解决 application*.yml 在 git / 本地的散乱管理：哪些入 git、哪些走 .gitignore、CI 怎么同步。

---

## 一、当前现状（2026-07-02 盘点）

### 1.1 7 个 yml 文件归属

| 文件 | 状态 | 内容性质 | 应入 git？ |
|---|---|---|---|
| `config/application.yml` | **untracked**（磁盘有） | 基础配置（port / session / profile / mybatis-plus / app 元信息），**无敏感** | **应入** |
| `config/application-dev.yml` | **已入 git** | localhost 调试配置，**含 `root/root123456` 硬编码密码** | **应脱敏后入** |
| `config/application-etc.yml` | **已入 git** | 通用部署配置，**全用 `${ENV:default}` 占位** | **应入** |
| `config/application-oss.yml` | **已入 git**（仅 admin） | 开源版标识，固定无变量 | **应入** |
| `config/application-nas.yml` | **不存在** | 计划用于 NAS 部署 | **应新建** |
| `config/application-pro.yml` | 不存在 | 计划用于商业版 | **按需** |
| `application*.local.yml` | .gitignore 已忽略 | 个人覆盖 | **不入** |

### 1.2 已知问题

1. **`application.yml` 长期 untracked**——新克隆后需手动从别处复制，否则启动报 `Could not resolve placeholder 'spring.profiles.active'`
2. **`application-dev.yml` 含硬编码密码**——已入 git，存在**安全风险**（任何 git 历史 viewer 都能看到）
3. **`.gitignore` 缺规则**——`application.yml` 和未来的 `application-nas.yml` 没被显式忽略，依赖"开发者不 add"的纪律
4. **2026-06-27 计划未落地**——`archive/2026-07/project-scan-and-todo-2026-07-02.md` 中记录的 application-nas.yml 创建和 application.yml gitignore 当时实际**未执行**

---

## 二、维护流程（规约）

### 2.1 命名约定

| 文件 | 用途 | 必入 git | 模板后缀 |
|---|---|---|---|
| `application.yml` | 公共基础（port/会话/分页/mybatis-plus/app 元信息） | ✅ | —— |
| `application-{profile}.yml` | profile 级覆盖（dev/oss/etc/nas/pro） | 视情况（见 2.2） | `.template` |
| `application-{profile}.local.yml` | 个人本地覆盖（IDE 调试用） | ❌（gitignore） | —— |

### 2.2 入 git 判断标准

**入 git**（满足任一）：
- 无敏感信息（IP / 端口 / 路径 / 业务常量）
- 敏感字段已用 `${ENV_VAR:default}` 占位（如 `${DB_PASSWORD:dev_default}`）

**不入 git**（满足任一）：
- 含真实账号密码 / API Key / 私钥 / 数据库连接串
- 是个人的本地调试配置

### 2.3 新增 profile 流程

> 例：新增 NAS 部署 profile（`application-nas.yml`）

1. **创建 yml**：`config/application-nas.yml`
2. **脱敏**：所有 IP / 账号 / 密码用 `${ENV:default}` 占位
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/jc_rag
       username: ${DB_USER:jc_rag}
       password: ${DB_PASSWORD:}
   ```
3. **入 git**：`git add config/application-nas.yml && git commit -m "feat(config): add nas profile"`
4. **更新本文档 §1.1 表格**（加一行 + 状态）
5. **CI 同步**：在 `.github/workflows/*.yml`（或 jenkinsfile）加入 `cp config/application-nas.yml ${DEPLOY_DIR}/`

### 2.4 修改现有 profile 流程

> 例：调整 `application-dev.yml` 的 DB 端口

1. **改前确认**：该文件是否在 git 跟踪内
   ```bash
   git ls-files jc-rag-backend/*/config/application-dev.yml
   ```
2. **在 git 内的**：直接改 + commit
3. **不在 git 内的**（untracked）：改后只更新本地，**不入 git**

### 2.5 敏感信息泄露应急

如发现 `application*.yml` 中含真实密码入 git：

1. **立刻** `git rm --cached config/application-dev.yml`（保留本地文件）
2. **改 yml** 用 `${ENV:default}` 占位
3. **改 .gitignore** 加 `config/application-dev.yml`
4. **重写 git 历史**：`git filter-branch --tree-filter 'rm -f config/application-dev.yml' HEAD`（**破坏性操作，需用户确认**）
5. **强制推送**：`git push --force-with-lease`（**高危，需用户确认**）
6. **通知团队**：所有人重新克隆

---

## 三、CI 同步策略

### 3.1 部署包构建（build-pro.sh）

```bash
# build-pro.sh 在 step 4 合并前端时, 同步:
cp jc-rag-community/jc-rag-backend/jc-rag-admin/config/application.yml \
   jc-rag-pro-enterprise/jc-rag-pro-app-admin/src/main/resources/

# profile 特定的 yml 不复制 (由启动命令指定 --spring.profiles.active)
```

### 3.2 启动时指定 profile

```bash
# dev
java -jar jc-rag-app-admin.jar --spring.profiles.active=dev

# 生产
java -jar jc-rag-app-admin.jar --spring.profiles.active=etc,nas

# 商业版
java -jar jc-rag-app-admin.jar --spring.profiles.active=etc,pro
```

### 3.3 部署目录结构

```text
/deploy/jc-rag-community/
├── jc-rag-app-admin.jar
├── application.yml                # 入 git, 基础配置
├── application-nas.yml            # 入 git, NAS 部署覆盖
└── .env                           # 不入 git, 真实密码 (chmod 600)
    DB_PASSWORD=xxx
    REDIS_PASSWORD=xxx
```

Spring Boot 自动加载 `${.env}` 配合 `spring.config.import: "optional:file:./.env"`

---

## 四、待补工作清单（不在本文档 PR 范围）

> 以下是 A5 评估时发现的遗留工作，**单独提 PR 处理**。

| # | 内容 | 文件 | 风险 | 建议优先级 |
|---|---|---|---|---|
| 1 | 创建 `application-nas.yml`（内容同 dev 改用占位） | `config/application-nas.yml` 新增 | 低 | 中 |
| 2 | `application.yml` 改用占位（去掉 `default-password: 123456`） | `config/application.yml` 改 | 低 | 中 |
| 3 | `application-dev.yml` 去掉硬编码密码 | `config/application-dev.yml` 改 | 中（已入 git） | **高** |
| 4 | `.gitignore` 加 `config/application-nas.yml` / `application-pro.yml` 注释 | `.gitignore` 改 | 低 | 低 |
| 5 | `git rm --cached application-dev.yml` 移出 git 跟踪 | git 操作 | 中（需重写历史） | **高** |
| 6 | `build-pro.sh` 增加 yml 复制逻辑 | `jc-rag-pro-script/build-pro.sh` 改 | 低 | 中 |
| 7 | CI workflow 加 yml 同步步骤 | `.github/workflows/*.yml` 新增 | 低 | 低 |

---

## 五、变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-07-02 | 首次建立本文档，盘 7 个 yml 状态 + 维护规约 | JC |
