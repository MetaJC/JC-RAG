# JC RAG 文档中心

> 所有项目文档统一在此目录, 按用途分类.

## 目录结构

```
docs/
├── README.md                              ← 本文件 (目录索引)
├── api.md                                 ← REST API 接口文档
├── startup-guide.md                       ← 启动指南 (开发环境)
│
├── architecture/                          ← 架构设计
│   ├── system-architecture.md             ← 系统整体架构
│   ├── modules.md                         ← 模块划分说明
│   ├── knowledge-parse-pipeline.md        ← 知识库解析流水线
│   ├── monorepo-edition.md                ← 单仓双版本架构 (背景)
│   └── pro-repository.md                  ← 商业版仓库说明
│
├── database/                              ← 数据库
│   ├── database-schema.md                 ← 数据字典 (表结构索引)
│   └── flyway.md                          ← Flyway 迁移规范
│
├── edition/                               ← 版本/发布相关
│   ├── a-to-c-migration.md                ← 商业版迁移指南
│   ├── migration-guide.md                 ← v1.0 拆分到独立公开仓完整流程
│   └── subtree-guide.md                   ← git subtree 拆分指南
│
├── ops/                                   ← 运维/部署
│   ├── deployment.md                      ← 部署手册
│   ├── release-guide.md                   ← 发布指南
│   ├── release.md                         ← 发布说明
│   ├── multi-platform-deploy.md           ← gitee + github 双平台部署
│   └── dev-template.md                    ← 开发规范模板
│
├── es/                                    ← Elasticsearch 相关
│   └── 001_jc_rag_all_in_one.http         ← ES 查询样例
│
├── test/                                  ← 测试用例
│   ├── README.md
│   ├── USAGE.md
│   ├── agent.md
│   ├── _template/                         ← 模板
│   ├── data/                              ← 测试数据
│   └── features/                          ← 功能测试用例
│
└── archive/                               ← 历史归档
    └── 2026-07/                           ← 2026 年 7 月归档
```

## 数据库迁移脚本位置

> **2026-07 更新**: SQL 迁移脚本已统一迁至 Flyway, 本目录不再保留 `sql/mysql/` 副本。
>
> 实际脚本路径: [`jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/`](../../jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/)
>
> 商业版额外加载: `jc-rag-pro/jc-rag-pro-backend/jc-rag-pro-core/jc-rag-pro/src/main/resources/db/migration/pro/`
>
> 加载机制详见: [database/flyway.md](database/flyway.md) 与 [商业版同步规范](../jc-rag-pro/jc-rag-pro-docs/edition/community-to-pro-sync.md)

## 文档维护原则

1. **代码变更 → 文档同步更新** (用户规则 5)
2. **新功能 → 必加文档**, 哪怕只是 README 一段
3. **架构调整 → 必更新 `architecture/` 下相关文档**
4. **破坏性变更 → 在 `release-guide.md` 写明迁移步骤**

## 快速索引

| 我想知道... | 看这个 |
|---|---|
| 项目是什么, 怎么跑起来 | `startup-guide.md` |
| 接口怎么调用 | `api.md` |
| 模块怎么划分 | `architecture/modules.md` |
| 系统整体架构 | `architecture/system-architecture.md` |
| 对话记忆 (L0/L1/L2/L3 + GDPR) | `architecture/memory-system.md` |
| 敏感词与安全防护 (词库/AC/PII/审计) | `architecture/sensitive-word.md` |
| 用户级智能体 (系统/自定义/默认) | `architecture/user-agent.md` |
| 知识库解析流水线 | `architecture/knowledge-parse-pipeline.md` |
| 商业版怎么集成 | `architecture/pro-repository.md` |
| 商业版 vs 社区版数据库同步 | `architecture/monorepo-edition.md` + [商业版同步规范](../jc-rag-pro/jc-rag-pro-docs/edition/community-to-pro-sync.md) |
| 怎么部署 | `ops/deployment.md` |
| 怎么发布 | `ops/release.md` / `ops/release-guide.md` |
| 怎么拆出公开仓 | `edition/migration-guide.md` / `edition/subtree-guide.md` |
| 怎么维护双平台 | `ops/multi-platform-deploy.md` |
| 表结构 | `database/database-schema.md` |
| Flyway 怎么跑 | `database/flyway.md` |
| ES 查询样例 | `es/001_jc_rag_all_in_one.http` |
| 测试用例 | `test/README.md` |

## 文档维护检查清单 (交付前必查)

- [ ] 修改代码时, 相关文档已同步
- [ ] 新增接口已加到 `api.md`
- [ ] 修改表结构时, 在 `jc-rag-admin/.../db/migration/community/` 新增 V 脚本, 同步 `database/database-schema.md`
- [ ] 修改部署时, `ops/deployment.md` 同步
- [ ] 修改发布流程时, `ops/release-guide.md` 同步
- [ ] 涉及商业/开源边界时, `architecture/pro-repository.md` + 商业版 `boundary.md` 同步
- [ ] 涉及社区版→商业版同步冲突, 在商业版 `edition/community-to-pro-sync.md` 追加记录
