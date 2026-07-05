# JC RAG Pro 商业版仓库结构

> 本文档说明 `jc-rag-pro` 私有仓库的目录组织与模块职责.

## 目录

```
jc-rag-pro/
│
├── jc-rag-community/                                   ← 开源版 (内嵌, v1.0 拆为独立仓)
│
├── jc-rag-pro-core/                          ← 商业付费代码 (不入开源仓库)
│   ├── jc-rag-pro-backend/                   ← Java 商业版核心模块
│   │   └── jc-rag-pro/                       ← Spring Boot 子模块
│   │       ├── pom.xml
│   │       └── src/main/java/com/jc/rag/pro/
│   │           ├── controller/                ← 商业版专属 Controller
│   │           │   ├── ProExclusiveController      → /api/admin/pro-exclusive/meta
│   │           │   ├── ProKbSearchAdvancedController → /api/admin/pro/kb-search-advanced/*
│   │           │   ├── ProKbEvalController          → /api/admin/pro/kb-eval/*
│   │           │   └── ProKbAnalyticsController     → /api/admin/pro/kb-analytics/*
│   │           ├── service/impl/              ← 商业版增强实现
│   │           │   └── ProFeatureServiceProImpl    → 覆盖 community 的 OssImpl
│   │           ├── kb/search, kb/eval, kb/analytics  ← 商业版专属领域包
│   │           ├── feature/                   ← 商业版特性开关
│   │           └── license/                   ← License 校验
│   │           └── LicenseContext.java
│   │
│   ├── jc-rag-pro-frontend/                  ← 前端商业版扩展
│   │   ├── package.json                       ← 独立 npm 项目
│   │   ├── pro-vite.config.ts                 ← 独立 vite 配置
│   │   └── src/
│   │       ├── pro-router.ts                  ← 商业版路由扩展点 (覆盖 jc-rag 的 pro-extension.ts)
│   │       ├── pro-views/                     ← 商业版专属 Vue 组件
│   │       │   ├── KnowledgeProView.vue
│   │       │   ├── KnowledgeAuthProView.vue
│   │       │   ├── KbSearchAdvancedView.vue
│   │       │   ├── KbEvalView.vue
│   │       │   ├── KbAnalyticsView.vue
│   │       │   └── index.ts
│   │       ├── pro-api/                       ← 商业版 API 客户端
│   │       │   └── index.ts
│   │       └── pro-components/                ← 商业版共享组件
│   │
│   └── jc-rag-pro-service/                   ← Python 商业版服务
│       ├── pyproject.toml
│       ├── advanced_search/                   ← 高级检索 (vector + BM25 + rerank)
│       ├── eval/                              ← RAG 评测
│       └── analytics/                         ← 知识库分析
│
├── jc-rag-pro-enterprise/                    ← 商业版编排层 (双启动模块)
│   ├── jc-rag-pro-app-admin/                 ← Admin 端启动模块 (port 8080)
│   │   ├── pom.xml                            ← 依赖 jc-rag-admin + jc-rag-pro
│   │   ├── docker/Dockerfile
│   │   └── src/main/
│   │       ├── java/com/jc/rag/admin/AdminProApplication.java
│   │       └── resources/
│   │           ├── application.yml            ← 默认 pro 模式
│   │           ├── application-pro.yml
│   │           └── static/                    ← 前端构建产物 (build-pro.sh 注入)
│   ├── jc-rag-pro-app-user/                  ← User 端启动模块 (port 8082)
│   │   ├── pom.xml                            ← 依赖 jc-rag-user + jc-rag-pro
│   │   ├── docker/Dockerfile
│   │   └── src/main/...
│   ├── docker-compose.pro.yml                 ← Docker 编排
│   └── README-pro.md                          ← 商业版部署文档
│
├── jc-rag-pro-script/                        ← 构建 / 运维脚本
│   ├── build-pro.sh / .ps1                    ← 一键构建商业版
│   ├── update-community.sh / .ps1             ← (v1.0 才用) 同步开源版代码
│   └── README.md
│
├── .gitignore                                 ← 忽略商业敏感 + 构建产物
└── README.md
```

## 关键文件说明

| 文件 | 作用 | 关键 |
|------|------|------|
| `jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/pom.xml` | Java 商业版核心 pom | 继承 spring-boot-starter-parent 3.3.5 |
| `jc-rag-pro-core/jc-rag-pro-frontend/pro-vite.config.ts` | 前端商业版构建配置 | 仅编译 pro 扩展, 不重复打包 community 基础 |
| `jc-rag-pro-core/jc-rag-pro-frontend/src/pro-router.ts` | 商业版路由 | build-pro.sh 覆盖 jc-rag 的 pro-extension.ts |
| `jc-rag-pro-enterprise/jc-rag-pro-app-admin/pom.xml` | Admin 商业版启动模块 | 依赖 jc-rag-admin + jc-rag-pro, repackage 成 fat jar |
| `jc-rag-pro-enterprise/jc-rag-pro-app-admin/src/main/resources/application.yml` | 商业版默认配置 | `jc-rag.edition: pro` |
| `jc-rag-pro-script/build-pro.sh` | 一键构建脚本 | 8 步: jc-rag install → jc-rag-pro install → 前端合并 → 双 enterprise package |
| `jc-rag-pro-script/update-community.sh` | (v1.0 才用) 同步开源版 | `git subtree pull` + 健康检查 |

## 与开源仓库的对应关系

| 私有仓库路径 | 公开仓库对应路径 | 关系 |
|--------------|------------------|------|
| `jc-rag-community/` | 整个公开仓库 (v1.0 拆出) | 当前内嵌, 未来用 git subtree 拆为独立仓 |
| `jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/src/main/java/com/jc/rag/pro/` | 无 | 商业版独占 |
| `jc-rag-pro-core/jc-rag-pro-frontend/src/` | 无 | 商业版独占 |
| `jc-rag-pro-core/jc-rag-pro-service/` | 无 | 商业版独占 |
| `jc-rag-pro-enterprise/jc-rag-pro-app-{admin,user}/` | 无 | 商业版独占 (双启动模块) |
| `jc-rag-pro-script/` | 无 | 商业版独占 (公开仓库有自己的 scripts/) |
| 无 (在 jc-rag-community/ 里) | `jc-rag-backend/jc-rag-common/.../feature/ProFeatureService.java` | 接口由 jc-rag 定义, jc-rag-pro 实现 |
| 无 (在 jc-rag-community/ 里) | `jc-rag-backend/jc-rag-admin/.../feature/service/impl/ProFeatureServiceOssImpl.java` | 开源版默认实现, pro 覆盖 |
| 无 (在 jc-rag-community/ 里) | `jc-rag-frontend/jc-rag-admin-web/src/router/pro-extension.ts` | 路由扩展点, pro-router.ts 覆盖 |

## 开发工作流

### 修改开源版 (community 仓库)

```bash
# 1. 进入 jc-rag (开源版) 目录
cd jc-rag

# 2. 切新分支 / 提交
git checkout -b feat/xxx
git commit -am "feat: xxx"
git push origin feat/xxx

# 3. 提 PR → 合并到 jc-rag main (v1.0 拆出后的公开仓)

# 4. 回到 pro 仓库, 同步
cd ..
./jc-rag-pro-script/update-community.sh   # v1.0 才用, v0.x 跳过
```

### 修改商业版 (pro 仓库)

```bash
# 1. 在 jc-rag-pro-core/ 目录开发
vim jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/src/main/java/com/jc/rag/pro/...
vim jc-rag-pro-core/jc-rag-pro-frontend/src/pro-views/KnowledgeProView.vue

# 2. 提交 (商业版独立分支 / 直接 main)
git add jc-rag-pro-core/
git commit -m "feat(pro): xxx"

# 3. 推送 (商业版内部仓库)
git push origin main
```

### 本地完整测试

```bash
./jc-rag-pro-script/build-pro.sh
java -jar jc-rag-pro-enterprise/jc-rag-pro-app-admin/target/jc-rag-app-admin-enterprise.jar
```
