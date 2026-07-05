# 发布到生产环境（etc）操作手册

> 本流程适用于把本地 Windows 环境的开发成果发布到生产 Linux/Windows 服务器。
> 生产环境使用 `application-etc.yml` 配置。

---

## 一、发布物清单

### 管理端（jc-rag-admin）

```
jc-rag-admin/
├── jc-rag-admin.jar
└── config/
    ├── application.yml
    ├── application-dev.yml
    └── application-etc.yml
```

### 用户端（jc-rag-user）

```
jc-rag-user/
├── jc-rag-user.jar
└── config/
    ├── application.yml
    ├── application-dev.yml
    └── application-etc.yml
```

### Python RAG 服务

```
rag-service/
├── app/
├── main.py
├── rag_config.yml
├── requirements.txt
└── .env
```

> 前端 `npm run build` 会自动输出到对应后端 `src/main/resources/static/`，打包 jar 时已包含。

---

## 二、本地发布（一键脚本）

### 1. 准备
- 确认本地代码已提交/保存
- 确认桌面存在 `rag_release` 文件夹（脚本会自动创建）

### 2. 执行本地打包

```powershell
# 在项目根目录 d:\Project\jc-rag 下执行
.\scripts\release-local.ps1
```

脚本会自动完成：
1. 构建前端 1：`web\jc-rag-admin-web` → 输出到 `server\jc-rag-admin\src\main\resources\static`
2. 构建前端 2：`web\jc-rag-user-web` → 输出到 `server\jc-rag-user\src\main\resources\static`
3. install 公共模块 `jc-rag-common`（保证下游模块能找到新字段）
4. 打包 jar 1：`server\jc-rag-admin` → `target\*.jar`
5. 打包 jar 2：`server\jc-rag-user` → `target\*.jar`
6. 收集 Python 服务必要文件
7. 收集 Spring 配置（`config/` 目录，含 dev/etc profile）
8. **拷贝到桌面 `rag_release\`**，结构如下：
   - `rag_release\jc-rag-admin\jc-rag-admin.jar`
   - `rag_release\jc-rag-admin\config\` (含 application.yml/dev/etc)
   - `rag_release\jc-rag-user\jc-rag-user.jar`
   - `rag_release\jc-rag-user\config\` (含 application.yml/dev/etc)
   - `rag_release\rag-service\` (Python 完整目录)
   - `rag_release\release-info.txt`（版本时间戳）

---

## 三、服务器部署

### 1. 上传文件
把桌面 `rag_release\` 整个目录上传到服务器，例如 `/opt/rag_release/`。

### 2. 执行部署脚本

```bash
# 在服务器上、rag_release 同级目录执行
# 或指定路径：./scripts/release-server.sh /opt/rag_release /opt/rag
./scripts/release-server.sh
```

脚本会自动完成：
1. 停止现有 Java 服务（`jc-rag-admin`、`jc-rag-user`）
2. 停止现有 Python RAG 服务
3. 备份旧 jar 到 `backup/`（带时间戳）
4. 拷贝新 jar 到部署目录
5. 拷贝新 `config/` 覆盖部署目录
6. 同步新 `rag-service/` 代码（不动 venv、data）
7. 启动 Java 服务（带 `--spring.profiles.active=etc`）
8. 启动 Python RAG 服务
9. 健康检查（10s 等待后探测）

### 3. 服务器环境变量（etc profile 必需）

服务器需预先设置以下环境变量（写入 `/etc/profile.d/rag.sh`）：

```bash
export DB_HOST=10.1.2.3
export DB_PORT=3306
export DB_USER=jc_rag
export DB_PASSWORD=your_db_password
export REDIS_HOST=10.1.2.4
export REDIS_PORT=6379
export REDIS_PASSWORD=
export PYTHON_RAG_URL=http://127.0.0.1:8001
export MINIO_ENDPOINT=http://10.1.2.6:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin_secret
export MINIO_BUCKET=jc-rag-docs
export ES_URL=http://10.1.2.7:9200
```

`source /etc/profile.d/rag.sh` 后再启动服务。

---

## 四、回滚

服务器脚本每次部署前自动备份到 `backup/jc-rag-admin-YYYYMMDD-HHmmss.jar`。
回滚命令：

```bash
# 停服
./scripts/stop-all.sh

# 恢复（按需修改时间戳）
cp backup/jc-rag-admin-20260101-120000.jar /opt/rag/server/jc-rag-admin.jar
cp backup/jc-rag-user-20260101-120000.jar /opt/rag/user/jc-rag-user.jar

# 启动
./scripts/start-all.sh --profile etc
```

---

## 五、目录约定（脚本假设）

| 用途 | Windows 本地路径 | Linux 服务器路径（默认） |
|---|---|---|
| 项目根 | `d:\Project\jc-rag` | `/opt/rag` |
| 后台 jar 部署 | - | `/opt/rag/server/` |
| 用户 jar 部署 | - | `/opt/rag/user/` |
| Python 服务 | - | `/opt/rag/rag-service/` |
| Spring 配置 | - | 跟随 jar 同目录 `config/` |
| 桌面发布包 | `%USERPROFILE%\Desktop\rag_release` | - |
