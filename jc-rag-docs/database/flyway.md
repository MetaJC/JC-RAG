# Flyway 数据库迁移规范

> 本项目使用 Flyway 对 MySQL 数据库进行版本化迁移, 社区版与商业版均集成.

## 1. 目录约定

| 版本 | 路径 | 说明 |
|---|---|---|
| 社区版 | `jc-rag-community/jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/` | 社区版 + 商业版共享主源 |
| 商业版 | `jc-rag-pro/jc-rag-pro-backend/jc-rag-pro-core/jc-rag-pro/src/main/resources/db/migration/pro/` | 商业版专属增量, 社区版不要往这里放 |

### 1.1 命名规则
- 文件名: `V<版本号>__<描述>.sql`, **双下划线** 不可省略
- 描述用 snake_case, 简短说明本次变更内容
- 例: `V28__rbac_department_position_menu.sql`

### 1.2 版本号规范
- 社区版: `V1 ~ V99` (V1~V26 已合并入 V1 all-in-one, 当前 V27~V28 是分步脚本)
- 商业版: `V101+` 起, 避免与社区版冲突

## 2. SQL 编写规范

### 2.1 严禁
- ❌ **不要写 `USE jc_rag;` / `USE jc_rag_pro;`** — Flyway 库名由 `spring.datasource.url` 决定
- ❌ 不要在迁移脚本里写 DML 业务数据 (种子数据放 `V3__seed_data.sql` 之外需评估)
- ❌ 不要修改已执行的脚本 (Flyway checksum 校验会失败)

### 2.2 必须
- ✅ 所有 CREATE TABLE 必须有 `IF NOT EXISTS` (幂等)
- ✅ 所有 INSERT 必须有 `WHERE NOT EXISTS` 或 `ON DUPLICATE KEY UPDATE` (幂等)
- ✅ 表结构变更要同步更新本目录下 `database-schema.md` 文档
- ✅ 写中文注释说明本脚本目的 / 业务背景 / 设计权衡 (阿里规约)

## 3. 双版本同步机制

### 3.1 商业版如何包含社区版
商业版启动模块 (`jc-rag-pro-app-admin` / `jc-rag-pro-app-user`) 通过 Maven 依赖 `jc-rag-admin`:

```xml
<!-- jc-rag-pro/jc-rag-pro-backend/jc-rag-pro-app/jc-rag-pro-app-admin/pom.xml -->
<dependency>
    <groupId>com.jc</groupId>
    <artifactId>jc-rag-admin</artifactId>
</dependency>
```

`jc-rag-admin` 资源中的 `db/migration/community/` 自动进入 classpath, 商业版 Flyway 通过双 locations 加载:

```yaml
# jc-rag-pro/jc-rag-pro-backend/jc-rag-pro-app/jc-rag-pro-app-admin/src/main/resources/application.yml
spring:
  flyway:
    locations: classpath:db/migration/pro,classpath:db/migration/community
    baseline-on-migrate: true
    baseline-version: 27
```

商业版 Flyway 启动时按顺序执行:
1. `classpath:db/migration/community` (社区版 V1~V99)
2. `classpath:db/migration/pro` (商业版 V101+)

物理保证: 商业版任何数据库变更前, 社区版的所有变更已先执行.

### 3.2 新增 community 脚本后的同步步骤
1. 在 `community/V<新版本>__<描述>.sql` 写脚本
2. **同时** 在商业版 `jc_rag_pro` 库手动执行同一脚本 (避免商业版首次启动时 checksum 触发问题)
3. 删除 `flyway_schema_history` 中失败/历史脏记录
4. 重启商业版, Flyway 会自动识别并把脚本登记到 `flyway_schema_history` (NOT EXISTS 保证幂等)

## 4. baseline 配置

存量库 (已手工跑过 V1~V26) 首次启动时, 不希望 Flyway 重跑历史脚本, 设置 baseline:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 27  # V27 是当前第一个 Flyway 自动执行的脚本
```

- 当 Flyway 启动发现非空库且无 `flyway_schema_history` 表时, 在 `baseline-version` 指定的版本处打 baseline
- 后续 V28+ 才会被自动执行, V1~V26 视为已执行

## 5. 已执行版本 (截至 2026-07-13)

| V 号 | 描述 | 库 |
|---|---|---|
| 27 | rbac department position | 两库均已 baseline + 执行 |
| 28 | rbac department position menu | 两库均已手动同步, 等待重启自动登记 |

后续新增版本请在本表追加一行, 注明首次执行时间和执行人.

## 6. 常见问题

### 6.1 启动报 "Migration checksum mismatch"
原因: 已执行的脚本被修改, Flyway 校验失败.
解决: 不要改历史脚本! 如果必须改, 删除 `flyway_schema_history` 对应记录后重启 (但会重跑, 注意幂等).

### 6.2 启动报 "Migration failed"
原因: 脚本里有语法错误 / 字段不存在 / 表不存在.
解决:
1. 查看具体错误行号
2. 修复脚本
3. 手动在库中清理脏数据 (回滚到 baseline)
4. 删除 `flyway_schema_history` 中失败记录
5. 重启服务
