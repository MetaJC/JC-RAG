# jc-rag 运维手册（开源版 + 商业版）

> 覆盖启动、停止、日志、备份、升级、故障处理、监控、应急。

---

## 一、目录

1. [启动服务](#二启动服务)
2. [停止服务](#三停止服务)
3. [日志查看](#四日志查看)
4. [数据备份](#五数据备份)
5. [服务升级](#六服务升级)
6. [故障处理](#七故障处理)
7. [监控告警](#八监控告警)
8. [应急操作](#九应急操作)
9. [License 管理](#十license-管理-仅商业版)

---

## 二、启动服务

### 2.1 开源版启动

#### 方式 A: 完整 Docker（推荐生产）
```bash
# 进入开源版根目录
cd D:\Project\jc-rag\community   # v0.x 当前路径
# cd D:\Project\jc-rag\jc-rag    # v1.0 改名后路径

# 启动所有服务
docker compose -f docker-compose.yml up -d

# 查看状态
docker compose -f docker-compose.yml ps

# 查看启动日志
docker compose -f docker-compose.yml logs -f
```

**包含服务**：
- mysql (3308)
- elasticsearch (9200)
- redis (6379)
- minio (9000/9001)
- etcd + minio + milvus (19530)
- jc-rag-admin (8080)
- jc-rag-user (8082)
- jc-rag-service Python (8000)

#### 方式 B: 本地 IDE 调试
```bash
# 1. 启基础设施
docker compose -f jc-rag-community/docker-compose.yml up -d mysql redis es minio etcd minio milvus

# 2. 起 Java (在 IDE 跑, 或命令行)
mvn -pl jc-rag-community/jc-rag-backend/jc-rag-admin -am spring-boot:run
mvn -pl jc-rag-community/jc-rag-backend/jc-rag-user -am spring-boot:run

# 3. 起 Python
cd jc-rag-community/jc-rag-service
source .venv/bin/activate  # Windows: .venv\Scripts\activate
uvicorn main:app --port 8000 --reload

# 4. 起前端 (开发模式)
cd jc-rag-community/jc-rag-frontend/jc-rag-admin-web
npm run dev  # 默认 5173
```

### 2.2 商业版启动

```bash
# 进入商业版编排目录
cd D:\Project\jc-rag                # 项目根
# cd D:\Project\jc-rag-pro          # v1.0 改名后

# 启动所有商业服务
docker compose -f docker-compose.pro.yml up -d

# 查看状态
docker compose -f docker-compose.pro.yml ps
```

**包含服务**：
- mysql / redis / milvus（基础）
- jc-rag-pro-app-admin (8080)
- jc-rag-pro-app-user (8082)
- jc-rag-pro-service Python (8001)
- （如需 jc-rag Python，加 `-f jc-rag-community/docker-compose.yml` 一起 up）

### 2.3 启动顺序（强依赖）

```
mysql → es/redis/minio → milvus (依赖 etcd) → Python/Java 业务
```

**禁止**：业务服务先于 mysql 启动，会因数据库连接失败崩溃。

### 2.4 健康检查

| 端点 | 用途 |
|---|---|
| `http://localhost:8080/actuator/health` | Java admin 健康检查 |
| `http://localhost:8082/actuator/health` | Java user 健康检查 |
| `http://localhost:8000/api/v1/health` | Python community 健康检查 |
| `http://localhost:8001/api/v1/health` | Python pro 健康检查 |
| `http://localhost:9001` | MinIO 控制台 |
| `http://localhost:9200/_cluster/health` | ES 健康检查 |
| `http://localhost:9091/healthz` | Milvus 健康检查 |

---

## 三、停止服务

### 3.1 标准停止
```bash
# 商业版
cd D:\Project\jc-rag
docker compose -f docker-compose.pro.yml down

# 开源版
cd D:\Project\jc-rag\community
docker compose -f docker-compose.yml down
```

### 3.2 停止并清数据（危险）
```bash
# 加 -v 删除 volumes, 数据全清
docker compose -f docker-compose.pro.yml down -v
```

### 3.3 优雅停单个服务
```bash
# 停 Java admin 不影响 user
docker compose -f docker-compose.pro.yml stop jc-rag-app-admin
```

### 3.4 停止本地 IDE 启动的服务
- IDE: 点 Stop 按钮
- 命令行: `Ctrl + C`

---

## 四、日志查看

### 4.1 Docker 日志
```bash
# 实时看所有服务
docker compose -f docker-compose.pro.yml logs -f

# 实时看单个服务
docker compose -f docker-compose.pro.yml logs -f jc-rag-app-admin

# 最近 100 行
docker compose -f docker-compose.pro.yml logs --tail=100 jc-rag-app-admin

# 时间范围
docker compose -f docker-compose.pro.yml logs --since "2026-06-29T10:00:00" jc-rag-app-admin
```

### 4.2 日志位置
| 服务 | 路径 |
|---|---|
| Java (Docker) | `docker logs <container>` |
| Java (IDE) | IDE Console |
| Python (Docker) | `docker logs <container>` |
| Python (IDE) | 终端输出 |
| MySQL | `/var/log/mysql/` 或 `docker logs mysql` |
| Milvus | `docker logs milvus-standalone` |
| ES | `/usr/share/elasticsearch/logs/` |

### 4.3 日志归档配置（logback）

Java 端 `logback-spring.xml` 已配置：
- 控制台输出
- 按天滚动文件（`logs/app-YYYY-MM-DD.log`）
- 30 天保留
- ERROR 单独文件 `logs/error-YYYY-MM-DD.log`

### 4.4 关键日志关键词

```
# 启动成功
"Started Application in X.XXX seconds"

# License 验签失败
"LICENSE_INVALID" / "License verification failed"

# 数据库连接失败
"Communications link failure"

# Milvus 连接失败
"Connection refused: milvus-standalone:19530"

# OOM
"OutOfMemoryError" / "GC overhead limit exceeded"
```

---

## 五、数据备份

### 5.1 MySQL 备份
```bash
# 完整备份
docker exec mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --all-databases' > backup/mysql-$(date +%Y%m%d).sql

# 单库备份
docker exec mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" jc_rag' > backup/jc_rag-$(date +%Y%m%d).sql

# 恢复
docker exec -i mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD"' < backup/jc_rag-20260629.sql
```

### 5.2 Milvus 备份
```bash
# Milvus 数据在 volume: milvus_data
# 备份整个 volume
docker run --rm \
    -v milvus_data:/source:ro \
    -v $(pwd)/backup:/backup \
    alpine tar czf /backup/milvus-$(date +%Y%m%d).tar.gz -C /source .

# 恢复
docker run --rm \
    -v milvus_data:/target \
    -v $(pwd)/backup:/backup \
    alpine tar xzf /backup/milvus-20260629.tar.gz -C /target
```

### 5.3 MinIO 备份
```bash
# 用 mc 客户端
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mirror local/jc-rag-bucket backup/minio-$(date +%Y%m%d)/
```

### 5.4 Elasticsearch 备份
```bash
# 注册仓库
curl -X PUT "localhost:9200/_snapshot/backup" -H 'Content-Type: application/json' -d'{
  "type": "fs",
  "settings": { "location": "/usr/share/elasticsearch/backup" }
}'

# 快照
curl -X PUT "localhost:9200/_snapshot/backup/snapshot-$(date +%Y%m%d)?wait_for_completion=true"
```

### 5.5 备份策略

| 数据 | 频率 | 保留 |
|---|---|---|
| MySQL | 每天 03:00 全量 | 30 天 |
| Milvus | 每周日全量 | 4 周 |
| MinIO | 每天增量 | 90 天 |
| ES | 每周日全量 | 4 周 |

### 5.6 备份脚本

`jc-rag-pro-script/backup-all.sh`（自动跑 cron）：
```bash
#!/bin/bash
set -e
BACKUP_DIR=/backup/$(date +%Y%m%d)
mkdir -p $BACKUP_DIR

# MySQL
docker exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases' > $BACKUP_DIR/mysql.sql

# Milvus
docker run --rm -v milvus_data:/source:ro -v $BACKUP_DIR:/backup alpine tar czf /backup/milvus.tar.gz -C /source .

# 压缩
tar czf $BACKUP_DIR.tar.gz $BACKUP_DIR
rm -rf $BACKUP_DIR

# 上传到 OSS（如配置）
# aliyun oss cp $BACKUP_DIR.tar.gz oss://jc-rag-backup/

# 清理 30 天前
find /backup -name "*.tar.gz" -mtime +30 -delete
```

---

## 六、服务升级

### 6.1 升级流程（标准）

```
1. 备份 → 2. 拉新代码 → 3. 跑测试 → 4. 重新构建镜像 → 5. 滚动重启 → 6. 验证
```

### 6.2 开源版升级

```bash
# 1. 备份
bash jc-rag-pro-script/backup-all.sh

# 2. 拉代码
cd D:\Project\jc-rag
git pull origin master

# 3. 跑测试
mvn -pl jc-rag-community/jc-rag-backend -am test
cd jc-rag-community/jc-rag-service && pytest

# 4. 重新构建
mvn -pl jc-rag-community/jc-rag-backend -am clean package -DskipTests
cd jc-rag-community/jc-rag-service && python -m build

# 5. 重启
cd jc-rag
docker compose down
docker compose up -d

# 6. 验证
curl http://localhost:8080/actuator/health
```

### 6.3 商业版升级

```bash
# 1. 备份
bash jc-rag-pro-script/backup-all.sh

# 2. 同步开源版代码（v1.0 之后从公开仓拉）
bash jc-rag-pro-script/sync-community.sh

# 3. 拉商业版代码
git pull origin master

# 4. 重新构建
bash jc-rag-pro-script/build-pro.sh

# 5. 滚动重启（先 user 后 admin, 减少停机）
cd D:\Project\jc-rag
docker compose -f docker-compose.pro.yml up -d --no-deps jc-rag-app-user
sleep 30  # 等待 user 健康
docker compose -f docker-compose.pro.yml up -d --no-deps jc-rag-app-admin

# 6. 验证
curl http://localhost:8080/actuator/health
```

### 6.4 数据库迁移

```bash
# SQL 脚本按版本号顺序执行
ls D:\Project\jc-rag\community\docs\sql\mysql\  # 已有 001-022 版本化脚本

# 应用前
docker exec -i mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" jc_rag' < docs/sql/mysql/023_new_feature.sql
```

**注意**：执行前必须看 SQL 内容，确认无破坏性变更。

### 6.5 灰度发布

- 用 nginx upstream 分流
- 50% 流量到 v0.1.0-pro.1，50% 到 v0.1.0-pro.2
- 监控 1 小时无异常 → 全量

---

## 七、故障处理

### 7.1 故障分类

| 等级 | 含义 | 响应时间 |
|---|---|---|
| P0 | 核心服务全挂 | 立即 (5 分钟) |
| P1 | 商业版不可用 | 30 分钟 |
| P2 | 部分功能异常 | 2 小时 |
| P3 | 性能/UI 问题 | 1 天 |

### 7.2 常见故障

#### F1: Java 启动失败
```bash
# 1. 看日志
docker logs jc-rag-app-admin

# 2. 常见原因
# - 数据库连接失败 → 检查 mysql 状态
# - 端口占用 → netstat -ano | findstr 8080
# - License 失效 → 重新生成

# 3. 解决
docker compose -f docker-compose.pro.yml restart jc-rag-app-admin
```

#### F2: License 验签失败
```
错误: "License verification failed" / "LICENSE_INVALID"
原因:
  1. License 过期 → 用 gen-license.sh 重新生成
  2. License 文件被改 → 从备份恢复
  3. License 与公钥不匹配 → 重新生成
```

#### F3: Milvus 连接失败
```bash
# 1. 检查 milvus 状态
docker logs milvus-standalone

# 2. 常见原因
# - etcd 未起 → 启 etcd
# - 磁盘满 → df -h
# - OOM → 加内存

# 3. 重启 milvus
docker compose restart milvus-standalone
```

#### F4: MySQL 连接数爆满
```sql
-- 查看连接
SHOW PROCESSLIST;
-- 杀空闲连接
KILL <ID>;
-- 调 max_connections
SET GLOBAL max_connections = 500;
```

#### F5: ES 集群 Yellow/Red
```bash
# Yellow: 单节点正常, 多节点副本未分配
curl -X PUT "localhost:9200/_cluster/settings" -H 'Content-Type: application/json' -d'{
  "transient": {"cluster.routing.allocation.enable": "all"}
}'

# Red: 索引损坏, 需从备份恢复
```

#### F6: Python 服务 OOM
```bash
# 1. 看进程
docker stats jc-rag-python-pro

# 2. 加内存 (docker-compose.pro.yml)
services:
  jc-rag-python-pro:
    deploy:
      resources:
        limits:
          memory: 4G
```

### 7.3 故障应急群

- 微信群：`jc-rag-ops`
- OnCall 轮值：见 `jc-rag-pro-docs/operations/oncall-rotation.md`
- 升级流程：P0 → 5 分钟无响应 → 打电话给 Tech Lead

---

## 八、监控告警

### 8.1 监控维度

| 维度 | 工具 | 指标 |
|---|---|---|
| 容器资源 | Prometheus + cAdvisor | CPU / 内存 / 磁盘 / 网络 |
| 应用健康 | Spring Boot Actuator | /actuator/health |
| API 响应 | Prometheus + Micrometer | p50 / p95 / p99 / QPS |
| 业务指标 | 自定义埋点 | DAU / 检索次数 / LLM 调用次数 |
| 日志 | Loki + Grafana | 错误日志 / 慢日志 |
| 告警 | Alertmanager | 阈值告警 / 异常检测 |

### 8.2 关键告警规则

```yaml
# alertmanager/rules.yml
groups:
- name: jc-rag
  rules:
  - alert: ServiceDown
    expr: up{job="jc-rag-admin"} == 0
    for: 1m
    severity: P0

  - alert: HighMemory
    expr: container_memory_usage_bytes > 4 * 1024 * 1024 * 1024
    for: 5m
    severity: P2

  - alert: SlowApi
    expr: http_request_duration_seconds{quantile="0.95"} > 1
    for: 5m
    severity: P2

  - alert: LicenseExpiringSoon
    expr: license_days_remaining < 7
    severity: P1
```

### 8.3 Grafana 仪表板

| 仪表板 | 内容 |
|---|---|
| jc-rag-overview | 全部服务总览 |
| jc-rag-java | Java JVM / GC / QPS |
| jc-rag-python | Python 进程 / LLM 调用 |
| jc-rag-milvus | Milvus 索引 / 检索 QPS |
| jc-rag-mysql | MySQL 连接 / QPS / 慢查询 |

---

## 九、应急操作

### 9.1 紧急回滚

```bash
# 1. 停止当前版本
docker compose -f docker-compose.pro.yml down

# 2. 切回上一版本
git checkout v0.1.0-pro.1
bash jc-rag-pro-script/build-pro.sh

# 3. 启动
docker compose -f docker-compose.pro.yml up -d

# 4. 验证
curl http://localhost:8080/actuator/health
```

### 9.2 数据损坏恢复

```bash
# 1. 停服务
docker compose -f docker-compose.pro.yml down

# 2. 恢复 MySQL
docker compose up -d mysql
sleep 30
docker exec -i mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < backup/mysql-20260629.sql

# 3. 恢复 Milvus
# 见 5.2

# 4. 启所有服务
docker compose -f docker-compose.pro.yml up -d
```

### 9.3 服务被攻击应急

```bash
# 1. 隔离
docker network disconnect jc-rag-net jc-rag-app-admin

# 2. 看攻击源
docker logs jc-rag-app-admin | grep -i "attack\|flood\|sql.injection"

# 3. 加白名单 (nginx)
# 修改 nginx.conf, 限制 IP

# 4. 重启
docker compose -f docker-compose.pro.yml restart nginx
```

---

## 十、License 管理（仅商业版）

### 10.1 License 生成

```bash
# 给客户生成 License
bash jc-rag-pro-script/gen-license.sh \
    -c "JC-Customer-001" \
    -e "2027-12-31" \
    -o jc-rag-pro-enterprise/jc-rag-pro-app-admin/src/main/resources/LICENSE-KEY
```

### 10.2 License 校验

启动时自动校验：
- 校验失败 → 日志记录 + 商业版功能禁用
- 校验成功 → 启动正常

### 10.3 License 续期

```bash
# 生成新 License
bash jc-rag-pro-script/gen-license.sh \
    -c "JC-Customer-001" \
    -e "2028-12-31" \
    -o LICENSE-KEY-new

# 替换客户服务器的 LICENSE-KEY
scp LICENSE-KEY-new customer@server:/path/to/LICENSE-KEY

# 重启服务
ssh customer@server "docker compose restart jc-rag-app-admin"
```

### 10.4 License 吊销

```bash
# 在客户服务器上删除 License
ssh customer@server "rm /path/to/LICENSE-KEY"
ssh customer@server "docker compose restart jc-rag-app-admin"
# 启动后商业版功能自动失效
```

---

## 十一、参考文档

- [项目总览](../architecture/overview.md)
- [发布流程](./release.md)
- [部署手册](./deployment.md)（v0.x 老文档, 保留作历史）
- [A→C 迁移指南](../edition/a-to-c-migration.md)
- [subtree 拆分指南](../edition/subtree-guide.md)
- [双平台部署](./multi-platform-deploy.md)
