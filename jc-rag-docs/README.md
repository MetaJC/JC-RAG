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
├── edition/                               ← 版本/发布相关
│   ├── subtree-guide.md                   ← git subtree 拆分指南
│   └── migration-guide.md                 ← v1.0 拆分到独立公开仓完整流程
│
├── ops/                                   ← 运维/部署
│   ├── deployment.md                      ← 部署手册
│   ├── release-guide.md                   ← 发布指南
│   └── multi-platform-deploy.md           ← gitee + github 双平台部署
│
├── es/                                    ← Elasticsearch 相关
│   └── 001_jc_rag_all_in_one.http         ← ES 查询样例
│
└── sql/mysql/                             ← MySQL 建表 SQL (按版本号顺序执行)
    ├── 001_jc_rag_all_in_one.sql          ← 初始化全量表
    ├── 002_modules.sql
    ├── 003_seed_data.sql
    ├── 004_management_merge_increment.sql
    ├── ...
    ├── 022_kb_permission.sql
    └── 023_user_memory_profile.sql        ← L3 跨会话画像表 (记忆系统)
```

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
| 商业版怎么集成 | `architecture/pro-repository.md` |
| 怎么部署 | `ops/deployment.md` |
| 怎么发布 | `ops/release-guide.md` |
| 怎么拆出公开仓 | `edition/migration-guide.md` |
| 怎么维护双平台 | `ops/multi-platform-deploy.md` |
| 表结构 | `sql/mysql/` |
| ES 查询样例 | `es/001_jc_rag_all_in_one.http` |

## 文档维护检查清单 (交付前必查)

- [ ] 修改代码时, 相关文档已同步
- [ ] 新增接口已加到 `api.md`
- [ ] 修改表结构时, `sql/mysql/` 顺序新增脚本
- [ ] 修改部署时, `ops/deployment.md` 同步
- [ ] 修改发布流程时, `ops/release-guide.md` 同步
- [ ] 涉及商业/开源边界时, `architecture/pro-repository.md` 同步
