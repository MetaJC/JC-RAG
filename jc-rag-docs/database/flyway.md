# Flyway 数据库迁移规范

> 社区版 `jc_rag` 的数据库结构以 Flyway 为唯一事实来源。商业版 `jc_rag_pro` 启动时自动加载社区版迁移, 但不得覆盖商业版专属结构。

## 1. 目录约定

| 版本 | 路径 | 说明 |
|---|---|---|
| 社区版 | `jc-rag-community/jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/` | 社区版主源, 也是商业版共享基线 |
| 商业版 | `jc-rag-pro/jc-rag-pro-backend/jc-rag-pro-core/jc-rag-pro/src/main/resources/db/migration/pro/` | 商业版专属增量与兼容补丁 |

## 2. 命名规则

### 2.1 社区版
- 文件名: `V<版本号>__<描述>.sql`
- 描述使用 snake_case
- 例: `V35__rbac_user_position.sql`

### 2.2 商业版
- 文件名: `V10000+__<描述>.sql`
- 当前约定从 `V10001` 开始, 预留给商业版长期独占
- 禁止与社区版复用同一个版本号

## 3. 自动同步原则

### 3.1 启动加载顺序
商业版启动模块依赖 `jc-rag-admin`, classpath 会同时拥有两套迁移目录:

```yaml
spring:
  flyway:
    locations: classpath:db/migration/community,classpath:db/migration/pro
```

执行原则:
1. 先执行社区版迁移, 保证商业版天然包含社区版能力
2. 再执行商业版迁移, 只做商业版专属结构和兼容补丁

### 3.2 社区版新增结构时怎么同步到商业版
1. 社区版新功能先在 `community/` 下新增 Flyway 脚本
2. 商业版重启时自动执行同一脚本到 `jc_rag_pro`
3. 如果商业版已有同名业务但实现不同:
   - 社区版脚本必须写成向后兼容的增量形式
   - 商业版再补一条 `V10000+` 兼容脚本兜底
   - 禁止让社区版脚本直接重建或覆盖商业版专属表

### 3.3 兼容设计红线
- 禁止在迁移脚本中 `DROP` 商业版专属表
- 禁止社区版脚本假设商业版与社区版字段完全一致
- 对已有表新增列、索引、默认值时优先使用增量方式
- 结构差异较大的商业版能力应落独立 `pro_` 表, 不与社区版表硬碰撞

## 4. SQL 编写规范

### 4.1 严禁
- 不要写 `USE jc_rag;` 或 `USE jc_rag_pro;`
- 不要修改已执行的历史脚本
- 不要把临时排障 SQL 当正式迁移脚本提交

### 4.2 必须
- DDL/DML 尽量幂等
- 迁移脚本顶部写清楚目的和兼容说明
- 表结构变更后同步更新社区版或商业版对应文档

## 5. baseline 说明

新建空库:
- 社区版 `jc_rag` 从 `V1` 开始完整执行
- 商业版 `jc_rag_pro` 从社区版 `V1` 开始, 再执行商业版 `V10001+`

存量库:
- 如果历史库曾人工执行过部分脚本, 只允许在确认现状后设置 baseline
- baseline 只用于接管旧库, 不能替代正常迁移

## 6. 本次规范化后的要求

- 社区版数据库结构变更只需要维护 `community/` 目录
- 商业版不得再要求“手工同步 community SQL”
- 商业版专属数据库变更只允许进入 `pro/` 目录
- 文档必须明确标注作用库: 社区版 `jc_rag` / 商业版 `jc_rag_pro`
