# JC RAG 双版本架构 (开源 + 商业)

> 本文档描述 JC RAG 的**开源版**与**商业版 Pro** 双仓库架构.  
> 适用于同一份基础代码同时维护开源免费版与商业付费版的场景.
>
> **架构方案**:
> - **v0.x**: 单仓 + community 内嵌 (旧版) — 见 [仓库布局 v0.x](#21-仓库布局-v0x-单仓内嵌)
> - **v0.7+ (当前, 方案 D)**: **monorepo + pnpm workspace** (前端) + **Maven 多模块** (后端) — 见 [§ 11 monorepo 架构 (当前方案 D)](#11-monorepo-架构-当前方案-d)

---

## 1. 核心设计

| 维度 | 方案 | 关键点 |
|------|------|--------|
| **仓库结构** | **双仓库 + submodule** | `jc-rag-community` (公开, GitHub) + `jc-rag-pro` (私有, 内部 Git) |
| **商业代码隔离** | 物理隔离 | 商业代码、License、商业 UI **仅在私有仓库**, 公开仓库完全无商业痕迹 |
| **代码同步方向** | **单向**: 开源 → 商业 | `git submodule update --remote` 一行命令拉取 |
| **商业版 API 隔离** | Maven 依赖 | 商业版启动模块 `jc-rag-app-enterprise` 依赖 community + pro 两个本地 jar |
| **同一功能两版本** | 接口 + 双实现 + 条件注入 | `ProFeatureService` 接口在 community, `OssImpl` 在 community, `ProImpl` 在 pro |
| **商业专属功能** | 放 pro 仓库 | 开源版根本看不到, 访问返回 404 |
| **License 控制** | RSA 签名 + 远程校验 | `jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/src/main/resources/LICENSE-KEY` |

---

## 2. 仓库布局

### 公开仓库 `jc-rag-community`

```
jc-rag-community/                              ← 公开, GitHub (v1.0 subtree 拆出)
├── jc-rag-frontend/                            ← Vue 3 前端
│   ├── jc-rag-admin-web/                       ← 管理后台
│   └── jc-rag-user-web/                        ← C 端
├── jc-rag-backend/                             ← Java 后端
│   ├── jc-rag-common/                          ← 公共代码 + 接口定义
│   ├── jc-rag-domain/                          ← 领域层
│   ├── jc-rag-admin/                           ← 管理端
│   └── jc-rag-user/                            ← C 端
├── jc-rag-service/                             ← Python RAG
├── jc-rag-docs/                                ← 文档 (含本文件)
└── README.md
```

### 私有仓库 `jc-rag-pro` (当前单仓, 未来双仓)

> 现状 (v0.x): 单仓结构, 开源版以 `jc-rag-community/` 子目录形式内嵌, **不**使用 submodule.  
> 未来 (v1.0): 用 `git subtree` 把 `jc-rag-community/` 拆出成独立公开仓库.

```
jc-rag-pro/                                    ← 私有, 内部 Git (项目代号, 实际根目录名可能是 jc-rag)
├── jc-rag-community/                                    ← ★ 开源版 (内嵌, v1.0 拆为独立仓) ★
├── jc-rag-pro-core/                           ← ★ 商业付费代码 ★
│   ├── jc-rag-pro-backend/                    ← Java 商业版
│   │   └── jc-rag-pro/                        ← 子模块 (kb-search-advanced / kb-eval / kb-analytics / License)
│   ├── jc-rag-pro-frontend/                   ← 前端商业版扩展 (pro-router / pro-views / pro-api)
│   ├── jc-rag-pro-service/                    ← Python 商业版 (advanced_search / eval / analytics)
│   └── LICENSE-KEY                            ← 商业 license
├── jc-rag-pro-enterprise/                     ← ★ 商业版编排层 (双启动模块) ★
│   ├── jc-rag-pro-app-admin/                  ← Admin 端启动 (port 8080)
│   ├── jc-rag-pro-app-user/                   ← User 端启动 (port 8082)
│   ├── docker-compose.pro.yml                 ← Docker 编排
│   └── README-pro.md                          ← 商业版文档
├── jc-rag-pro-script/                         ← 商业版构建脚本
│   ├── build-pro.sh / .ps1                    ← 一键构建
│   └── update-community.sh / .ps1             ← (v1.0 才用) 同步开源版
├── .gitmodules                                ← (v1.0 才用) submodule 配置
└── README.md
```

---

## 3. 接口 + 双实现模式 (核心)

### 3.1 在 community (公开仓库) 定义接口

```java
// jc-rag-community/jc-rag-backend/jc-rag-common/.../feature/ProFeatureService.java
public interface ProFeatureService {
    ProAnalysisResult analyze(Long kbId);
    default boolean isProEnhanced() { return false; }
}
```

### 3.2 在 community 实现开源版默认逻辑

```java
// jc-rag-community/jc-rag-backend/jc-rag-admin/.../feature/service/impl/ProFeatureServiceOssImpl.java
@Service
@ConditionalOnProperty(name = "jc-rag.edition", havingValue = "oss", matchIfMissing = true)
@ConditionalOnMissingBean(name = "proFeatureServiceProImpl")
public class ProFeatureServiceOssImpl implements ProFeatureService {
    public ProAnalysisResult analyze(Long kbId) {
        return ProAnalysisResult.builder().proEnhanced(false).build();  // 占位
    }
}
```

### 3.3 在 pro (私有仓库) 实现商业版增强逻辑

```java
// jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/.../service/impl/ProFeatureServiceProImpl.java
@Service
@ConditionalOnProperty(name = "jc-rag.edition", havingValue = "pro")
public class ProFeatureServiceProImpl implements ProFeatureService {
    public ProAnalysisResult analyze(Long kbId) {
        return ProAnalysisResult.builder()
            .proEnhanced(true)
            .satisfactionScore(87)
            .build();  // 商业增强
    }
}
```

### 3.4 调用方零感知

```java
@RestController
@RequiredArgsConstructor
public class ProFeatureController {
    private final ProFeatureService proFeatureService;  // Spring 选实现

    @GetMapping("/api/admin/pro-feature/analyze")
    public R<ProAnalysisResult> analyze(Long kbId) {
        return R.ok(proFeatureService.analyze(kbId));
    }
}
```

**结果**:
- 启动 community 开源版 jar → 注入 `OssImpl` → 返回占位结果
- 启动 enterprise 商业版 jar → 注入 `ProImpl` → 返回增强结果

---

## 4. 商业专属功能

无需开源 fallback, 直接放 pro 仓库. 开源版启动时这些 Controller 不在 classpath, 访问返回 404.

```java
// jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/.../controller/ProExclusiveController.java
@RestController
@RequestMapping("/api/admin/pro-exclusive")
public class ProExclusiveController {
    @GetMapping("/meta")
    public R<Map<String, Object>> meta() { ... }  // 仅商业版可访问
}
```

```java
// jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/.../kb/search/ProKbSearchAdvancedController.java
@RestController
@RequestMapping("/api/admin/pro/kb-search-advanced")
public class ProKbSearchAdvancedController {
    @GetMapping("/search")
    public R<Map<String, Object>> search(...) { ... }  // 高级检索
}
```

---

## 5. 前端双版本方案

### 5.1 community 定义路由扩展点 (空数组)

```typescript
// jc-rag-community/jc-rag-frontend/jc-rag-admin-web/src/router/pro-extension.ts
// ★★★ 商业版构建脚本 (build-pro.sh) 会覆盖此文件 ★★★
import type { RouteRecordRaw } from 'vue-router'

// 开源版: 空数组 (无商业扩展)
export const proExtensionRoutes: RouteRecordRaw[] = []
```

```typescript
// jc-rag-community/jc-rag-frontend/.../router/index.ts
import { proExtensionRoutes } from './pro-extension'

const ossRoutes: RouteRecordRaw[] = [
    { path: '/knowledge', component: () => import('@/views/oss/KnowledgeView.vue') },
]

// 主路由 = 开源版路由 + 商业版扩展路由 (后注册覆盖同 path)
export const routes: RouteRecordRaw[] = [...ossRoutes, ...proExtensionRoutes]
```

### 5.2 pro 提供商业版路由

```typescript
// jc-rag-pro-core/jc-rag-pro-frontend/src/pro-router.ts
import type { RouteRecordRaw } from 'vue-router'

export const proExtensionRoutes: RouteRecordRaw[] = [
    {   // 覆盖开源版 /knowledge
        path: '/knowledge',
        component: () => import('./pro-views/KnowledgeProView.vue'),
        meta: { title: '知识库 (Pro)', edition: 'pro' }
    },
    {   // 商业版新增
        path: '/pro/kb-search-advanced',
        component: () => import('./pro-views/KbSearchAdvancedView.vue'),
        meta: { title: '高级检索 (Pro)', edition: 'pro' }
    },
]
```

### 5.3 community .gitignore 排除

```gitignore
# 商业版构建时注入, 不入 community git
jc-rag-community/jc-rag-frontend/jc-rag-admin-web/src/pro/
jc-rag-community/jc-rag-frontend/jc-rag-admin-web/src/router/pro-extension.ts.built
```

---

## 6. 商业版构建脚本

### `jc-rag-pro-script/update-community.sh`

```bash
#!/usr/bin/env bash
# 同步 jc-rag (v0.x 内嵌, v1.0 拆出后用 subtree pull)
cd jc-rag
git fetch --tags
git checkout main
git pull --rebase --autostash
cd ..
git add jc-rag
git commit -m "chore: sync jc-rag to $(cd jc-rag && git rev-parse --short HEAD)"
```

### `jc-rag-pro-script/build-pro.sh` (关键流程)

```bash
#!/usr/bin/env bash
set -e

# 1. community 后端 install
cd jc-rag-community/jc-rag-backend
mvn clean install -DskipTests
cd ../..

# 2. pro 后端 install
cd jc-rag-pro-core/jc-rag-pro-backend
mvn clean install -DskipTests
cd ../..

# 3. 合并 pro 前端到 community 前端
cp -r jc-rag-pro-core/jc-rag-pro-frontend/src/pro-views/   jc-rag-community/jc-rag-frontend/jc-rag-admin-web/src/pro/
cp -r jc-rag-pro-core/jc-rag-pro-frontend/src/pro-api/     jc-rag-community/jc-rag-frontend/jc-rag-admin-web/src/pro/
cp jc-rag-pro-core/jc-rag-pro-frontend/src/pro-router.ts  jc-rag-community/jc-rag-frontend/jc-rag-admin-web/src/router/pro-extension.ts

# 4. 构建 community 前端 (pro 模式)
cd jc-rag-community/jc-rag-frontend/jc-rag-admin-web
npm install
npm run build -- --mode pro
cd ../../..

# 5. 复制前端 dist 到 enterprise 静态资源
rm -rf jc-rag-pro-enterprise/jc-rag-pro-app-admin/src/main/resources/static/*
rm -rf jc-rag-pro-enterprise/jc-rag-pro-app-user/src/main/resources/static/*
cp -r jc-rag-community/jc-rag-frontend/jc-rag-admin-web/dist/* jc-rag-pro-enterprise/jc-rag-pro-app-admin/src/main/resources/static/
cp -r jc-rag-community/jc-rag-frontend/jc-rag-admin-web/dist/* jc-rag-pro-enterprise/jc-rag-pro-app-user/src/main/resources/static/

# 6. 打包 enterprise 启动模块
cd jc-rag-pro-enterprise/jc-rag-pro-app-admin && mvn clean package -DskipTests
# user 端另起一行: cd jc-rag-pro-enterprise/jc-rag-pro-app-user && mvn clean package -DskipTests

echo "[DONE] 商业版 jar: target/jc-rag-app-enterprise.jar"
```

---

## 7. 版本同步流程

```
开发者 A 修改开源版 (community 仓库):
  ┌─────────────────────────────────────────────┐
  │ 1. 改 jc-rag-admin/.../KnowledgeManagerServiceOssImpl.java
  │ 2. 改 frontend/.../views/oss/KnowledgeView.vue
  │ 3. 改 jc-rag-common/.../KnowledgeManagerService.java (接口)
  │ 4. git commit + push → jc-rag-community 仓库
  └─────────────────────────────────────────────┘
              ↓ (商业仓库 CI/CD 触发 update-community.sh)
  ┌─────────────────────────────────────────────┐
  │ jc-rag-pro 仓库:
  │   ./scripts/update-community.sh
  │   → 拉取最新 community 代码
  │   → 编译测试: 商业版是否仍能跑通
  │   → 编译失败 → 商业版有责任修复
  └─────────────────────────────────────────────┘

开发者 B 修改商业版 (pro 仓库):
  ┌─────────────────────────────────────────────┐
  │ 1. 改 jc-rag-pro-core/jc-rag-pro-backend/.../KnowledgeManagerServiceProImpl.java
  │ 2. 改 jc-rag-pro-core/jc-rag-pro-frontend/src/pro-views/KnowledgeProView.vue
  │ 3. git commit + push → jc-rag-pro 仓库
  │ (开源版完全不受影响, pro 代码不进入 community)
  └─────────────────────────────────────────────┘
```

---

## 8. 关键原则

1. **公开仓库纯净** — community 仓库**不能有**任何商业代码 / 商业 UI / License 校验
2. **物理隔离** — 商业代码、License、配置**只在** pro 仓库
3. **接口在 community** — 公开 API 契约由 community 定义, 商业版必须遵守
4. **实现可分离** — 开源版实现在 community, 商业版实现在 pro
5. **公开 Controller 在 community** — 调用方零感知
6. **商业专属 Controller 在 pro** — 开源版访问 404
7. **构建产物分离** — 开源 `jc-rag-admin.jar`, 商业 `jc-rag-app-enterprise.jar`
8. **单向同步** — community → pro, 反方向绝不 push
9. **submodule 锁版本** — 生产环境用 `--tag v1.0.0` 锁定到稳定 tag
10. **构建脚本单向操作** — `build-pro.sh` 只在 pro 仓库执行, 不污染 community git

---

## 9. 验证清单

- [x] community 后端 `mvn clean install` 通过
- [x] pro 后端 `mvn clean install` 通过
- [x] enterprise 启动模块 `mvn clean package` 通过
- [x] community 前端 `npm run build -- --mode oss` 通过
- [x] community 前端 `npm run build -- --mode pro` (注入 pro 扩展后) 通过
- [ ] 开源版启动后访问 `/api/admin/pro-exclusive/meta` 返回 404
- [ ] 商业版启动后访问 `/api/admin/pro-exclusive/meta` 返回正常
- [ ] 商业版 `ProFeatureService` 注入的是 `ProImpl` (非 `OssImpl`)
- [ ] 前端构建期 `VITE_EDITION` 与后端 `/api/admin/edition` 返回一致
- [ ] `community/` 目录的 `git status` 干净 (无 pro 注入痕迹)

---

## 10. 相关文档

- [jc-rag-community/jc-rag-backend/jc-rag-admin/.../feature/](jc-rag-community/jc-rag-backend/jc-rag-admin/src/main/java/com/jc/rag/admin/module/feature/) — 开源版默认实现
- [jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/.../](jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/src/main/java/com/jc/rag/pro/) — 商业版增强实现
- [jc-rag-pro-enterprise/README-pro.md](jc-rag-pro-enterprise/README-pro.md) — 商业版部署文档
- [jc-rag-pro-script/README.md](jc-rag-pro-script/README.md) — 构建脚本说明

---

## 11. monorepo 架构 (当前方案 D)

> **生效版本**: v0.7+  · **方案 D**: pnpm workspace (前端) + Maven 多模块 (后端) · **替代 v0.x 单仓内嵌**

### 11.1 设计动机

v0.x 方案下, 商业版前端通过 `build-pro.sh` 把 pro 代码 `cp` 到 community 源码, 再以 community 名义 build. 风险:
- pro 代码物理进入 community 源码树, 第三方 clone community 可能误带 pro 痕迹
- pro 改 router 需要重建 community 的 vite 工程, 开发体验耦合
- vue 组件 / pinia store 跨版本复用只能靠 cp 注入, 无依赖管理

**方案 D** 用 pnpm workspace 让 pro 独立工程 `import` community 公共模块, 社区与商业前端彻底解耦, 后端沿用 Maven 多模块. **v1.0 subtree 拆 community 公开仓时, 该 monorepo 配置可平滑迁移.**

### 11.2 仓库布局 (方案 D)

```
jc-rag-pro/                                       ← 商业版仓库根
├── package.json                                  ← ★ monorepo 根 package.json ★
├── pnpm-workspace.yaml                          ← ★ workspace 声明 (community + pro 前端) ★
├── tsconfig.base.json                            ★ 共享 TS 配置
├── .npmrc                                        ★ pnpm 配置 (hoist / strict-peer-deps)
├── jc-rag-community/                             ← 开源版 (内嵌, v1.0 subtree 拆出)
│   ├── jc-rag-backend/                           ← Java 后端 (jc-rag-admin / jc-rag-user / jc-rag-common / jc-rag-domain)
│   ├── jc-rag-frontend/
│   │   ├── jc-rag-admin-web/                     ← community admin (workspace 子包)
│   │   └── jc-rag-user-web/                      ← community user  (workspace 子包)
│   ├── jc-rag-service/                           ← Python RAG
│   └── jc-rag-docs/
├── jc-rag-pro/                                   ← 商业版核心
│   ├── jc-rag-pro-backend/                       ← Java 后端 (Maven 多模块)
│   │   ├── jc-rag-pro-backend-aggregator/        ← 父 pom (artifactId: jc-rag-pro-backend-aggregator)
│   │   ├── jc-rag-pro-core/                      ← Pro 增强实现 (artifactId: jc-rag-pro)
│   │   └── jc-rag-pro-app/                       ← 启动模块聚合 (artifactId: jc-rag-pro-app-aggregator)
│   │       ├── jc-rag-pro-app-admin/             ← Admin 启动 (port 8080, 引用 jc-rag-admin + jc-rag-pro)
│   │       └── jc-rag-pro-app-user/              ← User 启动  (port 8082, 引用 jc-rag-user + jc-rag-pro)
│   ├── jc-rag-pro-frontend/                      ← ★ 商业版前端 (workspace 子包, 独立 SPA) ★
│   │   ├── src/router/index.ts                   ← 独立路由 (admin SPA)
│   │   ├── src/router.user.ts                    ← 独立路由 (user  SPA)
│   │   ├── src/pro-views/*.vue                   ← 商业专属 view
│   │   ├── src/pro-api/*.ts                      ← 商业专属 API
│   │   ├── package.json                          ← workspace:* 引用 community 子包
│   │   └── vite.config.ts                        ← alias 兜底 + mode=user 双入口
│   ├── jc-rag-pro-docs/                          ← 商业版专属文档
│   └── jc-rag-pro-script/
│       ├── build-pro.sh / .ps1                   ← 一键构建 (含 4 个 dist + 双 enterprise jar)
│       ├── gen-license.sh / .ps1
│       ├── package-release.ps1 / deploy-pro-server.sh ← 发布包收集 / 服务器部署
│       └── split-community-repo.sh               ← v1.0 拆 community 公开仓
├── docker-compose.pro.yml                        ← 商业版编排
├── .gitignore / .dockerignore                    ← monorepo 规则 (含 pnpm-store / 4 个 dist)
└── README.md
```

### 11.3 关键隔离机制

| 层 | 隔离手段 | 说明 |
|----|----------|------|
| **前端代码** | pnpm workspace + alias | pro 通过 `import 'jc-rag-admin-web/...'` 引用 community, 但 pro 自身 vite 工程独立, **pro 代码不进 community 源码树** |
| **前端打包** | 独立 SPA, 双 dist | community 各自 build → `admin-web/dist` + `user-web/dist`; pro 独立 build (admin / user 两 mode) → `pro-frontend/dist` |
| **后端代码** | Maven 模块 + 依赖方向 | pro-core / pro-app-admin / pro-app-user 三个 Maven 模块, 引用 community 已 install 的 `jc-rag-admin` / `jc-rag-user` jar, **不直接引 community 源码** |
| **后端 classpath** | 启动类扫描 | `AdminProApplication` (`com.jc.rag.pro`) `@ComponentScan(basePackages="com.jc.rag")` 扫整树, pro 增强实现靠 `@ConditionalOnProperty(jc-rag.edition=pro)` 激活 |
| **后端静态资源** | static/ 根目录直接覆盖 | pro 前端 dist 由 `build-pro.sh` 拷到启动模块 `src/main/resources/static/` 根目录 (覆盖 community 构建产物, pro 前端已含全部 community 页面); Spring Boot 默认映射 `classpath:/static/**`, 无需自定义 ResourceHandler |
| **后端 SPA 路由** | community SpaWebMvcConfig | history 模式子路由刷新 404 时, community `SpaWebMvcConfig` (ErrorViewResolver) 将非 API 路径 forward 到 `/index.html`; pro 启动模块依赖 community admin/user jar 自动继承, 无需 pro 专属配置 |
| **Security** | community `anyRequest().permitAll()` | 静态资源已被 community security 默认放行, **无需修改 community SecurityConfig** |
| **数据库** | 独立 datasource (下一阶段) | community + pro 各自 jc-rag-* 数据库, 通过 `application-pro.yml` 切换 (本阶段同源, 下一阶段独立) |

### 11.4 核心配置示例

#### 11.4.1 `pnpm-workspace.yaml` (根)

```yaml
packages:
  - 'jc-rag-community/jc-rag-frontend/jc-rag-admin-web'
  - 'jc-rag-community/jc-rag-frontend/jc-rag-user-web'
  - 'jc-rag-pro/jc-rag-pro-frontend'
```

#### 11.4.2 `pro/jc-rag-pro-frontend/package.json` (引用 community)

```json
{
  "name": "jc-rag-pro-frontend",
  "private": true,
  "dependencies": {
    "jc-rag-admin-web": "workspace:*",
    "jc-rag-user-web": "workspace:*",
    "vue": "^3.5.24",
    "vue-router": "^4.6.3",
    "pinia": "^3.0.4",
    "element-plus": "^2.11.8",
    "axios": "^1.13.2"
  }
}
```

#### 11.4.3 `pro/jc-rag-pro-backend/jc-rag-pro-app/jc-rag-pro-app-admin/pom.xml` (引用 community jar)

```xml
<dependencies>
  <dependency>
    <groupId>com.jc</groupId>
    <artifactId>jc-rag-admin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </dependency>
  <dependency>
    <groupId>com.jc</groupId>
    <artifactId>jc-rag-pro</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </dependency>
</dependencies>
```

### 11.5 构建流程 (`build-pro.sh`)

```
1. 校验 monorepo 根配置 (pnpm-workspace.yaml / pnpm 命令)
2. 校验 community 子目录
3. mvn install jc-rag-community/jc-rag-backend (community 4 模块)
4. mvn install jc-rag-pro/jc-rag-pro-backend (pro 5 模块, 含 admin/user jar)
5. pnpm install (workspace 全量, 软链 community 子包)
6. pnpm build 3 个前端:
   - jc-rag-admin-web  →  dist/
   - jc-rag-user-web   →  dist/
   - jc-rag-pro-frontend (admin) →  dist/index.html
   - jc-rag-pro-frontend (user)  →  dist/index.html (重命名为 user-index.html)
7. 复制 dist 到 enterprise 静态资源 (根路径单 SPA, 无嵌套前缀):
   - admin jar:  static/ ← pro admin dist (覆盖 community admin)
   - user  jar:  static/ ← pro user dist  (覆盖 community user)
8. mvn package 2 个 enterprise 启动模块 → 2 个 fat jar
```

### 11.6 验证记录 (本阶段)

| 项 | 状态 | 备注 |
|----|------|------|
| community 后端 mvn install | PASS | jc-rag-common / jc-rag-domain / jc-rag-admin / jc-rag-user 4 模块 |
| pro 后端 mvn install | PASS | jc-rag-pro + jc-rag-app-admin + jc-rag-app-user + 2 个聚合 pom 共 5 模块 |
| pnpm install (workspace) | PASS | 131 个包, community 子包软链到 pro 端 |
| pro 前端 vite build (admin) | PASS | 11.56s, 308 modules |
| pro 前端 vite build (user) | PASS | 13.18s, 308 modules |
| community 前端 vite build | PASS | 12.98s, community 自身 src 直接构建 (不通过 pro 入口) |
| community SecurityConfig 修改 | **NO** | 静态资源已被 `anyRequest().permitAll()` 覆盖, 无需改 community |
| community SpaWebMvcConfig 修改 | **NO** | pro 启动模块继承 community `SpaWebMvcConfig` 兜底 history 路由刷新 (v0.8 起取消 pro-static 嵌套前缀与 ProSpaRoutingConfig) |

### 11.7 与 v0.x 对比

| 维度 | v0.x (旧) | v0.7+ (方案 D) |
|------|-----------|----------------|
| community 源码纯净度 | ❌ pro 代码 cp 进 community, 需 gitignore 二次清理 | ✅ community src/ 完全无 pro 痕迹, 第三方 clone 干净 |
| pro 开发独立性 | ⚠️ 改 pro router 要重建 community vite 工程 | ✅ pro 独立 vite 工程, dev/build 自包含 |
| 跨包依赖管理 | ❌ 靠 cp 注入, 无版本管理 | ✅ pnpm workspace + version 协议 |
| v1.0 subtree 拆仓 | ⚠️ cp 注入脚本需要清 | ✅ monorepo 配置平滑迁出 |
| 构建时间 | community 重建 1 次 | 4 个独立 dist, 可并行 |
