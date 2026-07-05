# jc-rag 发布流程（开源版 + 商业版）

> 开源版 (jc-rag) 和商业版 (jc-rag-pro) 各自的发布步骤、版本号规范、自动化脚本。

---

## 一、版本号规范

| 类型 | 格式 | 示例 | 同步关系 |
|---|---|---|---|
| **开源版** | `MAJOR.MINOR.PATCH` | `0.1.0` | 公开仓统一版本号 |
| **商业版** | `MAJOR.MINOR.PATCH-pro.N` | `0.1.0-pro.1` | 跟开源版 `MAJOR.MINOR.PATCH`，`-pro.N` 商业版小版本 |
| **内部模块** | maven 习惯 | `0.1.0-SNAPSHOT` | 跟整体一致 |

**示例时间线**：
- community v0.1.0 → pro v0.1.0-pro.1
- community v0.2.0 → pro v0.2.0-pro.1（同步开源版升级）
- community v0.2.0 → pro v0.2.0-pro.2（商业版小功能）

---

## 二、开源版发布流程 (community)

### 2.1 前置条件
- [ ] 所有功能已 PR 合入
- [ ] 测试通过：`mvn test` + `pytest`
- [ ] 文档已更新（CHANGELOG.md + README.md）
- [ ] SQL 脚本已归档到 `docs/sql/mysql/`

### 2.2 发布步骤

#### Step 1: 升级版本号
```bash
# 修改 jc-rag-community/jc-rag-frontend/jc-rag-admin-web/package.json
# 修改 jc-rag-community/jc-rag-frontend/jc-rag-user-web/package.json
# 修改 jc-rag-community/jc-rag-backend/pom.xml <version>
# 修改 jc-rag-community/jc-rag-service/pyproject.toml version
```

#### Step 2: 跑测试
```bash
# Java
mvn -pl jc-rag-community/jc-rag-backend -am clean test

# Python
cd jc-rag-community/jc-rag-service
python -m pytest

# 前端
cd jc-rag-community/jc-rag-frontend/jc-rag-admin-web
npm run test
```

#### Step 3: 打包
```bash
# Java 端 (输出 jar)
mvn -pl jc-rag-community/jc-rag-backend -am clean package -DskipTests

# Python 端 (打 wheel)
cd jc-rag-community/jc-rag-service
python -m build

# 前端 (输出 dist)
cd jc-rag-community/jc-rag-frontend/jc-rag-admin-web
npm run build
```

#### Step 4: 打 tag
```bash
git add .
git commit -m "release: community v0.1.0"
git tag -a v0.1.0 -m "community v0.1.0"
git push origin master
git push origin v0.1.0
```

#### Step 5: 部署到公开仓（v1.0 之后）
```bash
# 假设 community 公开仓已建
bash jc-rag-pro-script/extract-community.sh
# 见 docs/edition/migration-guide.md
```

#### Step 6: 写 CHANGELOG
```bash
# 追加到 CHANGELOG.md
cat >> CHANGELOG.md << EOF
## [0.1.0] - $(date +%Y-%m-%d)
### Added
- ...
### Fixed
- ...
EOF
```

### 2.3 发布物清单

```
jc-rag-community/
├─ jc-rag-backend/
│  ├─ jc-rag-admin/target/jc-rag-admin.jar
│  ├─ jc-rag-user/target/jc-rag-user.jar
│  └─ ...
├─ jc-rag-service/
│  └─ dist/jc_rag_service-0.1.0-py3-none-any.whl
├─ jc-rag-frontend/
│  ├─ jc-rag-admin-web/dist/
│  └─ jc-rag-user-web/dist/
```

---

## 三、商业版发布流程 (pro)

### 3.1 前置条件
- [ ] 依赖的 community 版本已发布（如 pro v0.1.0-pro.1 依赖 community v0.1.0）
- [ ] 所有商业功能已实现并测试
- [ ] License 文件已生成（用 `gen-license.sh`）
- [ ] 一键构建脚本跑通：`bash jc-rag-pro-script/build-pro.sh`

### 3.2 发布步骤

#### Step 1: 同步开源版代码
```bash
# 把 community 最新代码同步到 pro 仓
bash jc-rag-pro-script/sync-community.sh
# (v1.0 之后从 community 公开仓拉; 现在是同仓内同步)
```

#### Step 2: 升级商业版版本号
```bash
# 修改 jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/pom.xml
# 修改 jc-rag-pro-core/jc-rag-pro-service/pyproject.toml
# 修改 jc-rag-pro-core/jc-rag-pro-frontend/package.json
# 格式: 0.1.0-pro.1
```

#### Step 3: 生成 License
```bash
# 给客户生成 License
bash jc-rag-pro-script/gen-license.sh \
    -c "JC-Customer-001" \
    -e "2027-12-31" \
    -o jc-rag-pro-enterprise/jc-rag-pro-app-admin/src/main/resources/LICENSE-KEY
```

#### Step 4: 一键构建
```bash
bash jc-rag-pro-script/build-pro.sh
# 内部步骤:
#   1. 合并 pro 路由到 jc-rag 前端
#   2. npm run build (jc-rag 模式 pro)
#   3. mvn clean package (pro jar)
#   4. python -m build (pro wheel)
#   5. 复制前端 dist 到 pro jar 的 static/
#   6. 输出 dist/ 目录
```

#### Step 5: 验证
```bash
# 起 jc-rag 服务（开发态）
cd jc-rag
docker compose -f docker-compose.yml up -d  # 基础服务 + jc-rag Python
mvn -pl jc-rag-backend -am spring-boot:run  # jc-rag Java

# 起 pro 服务
cd jc-rag-pro-enterprise
mvn -pl jc-rag-pro-app-admin spring-boot:run -Dspring-boot.run.profiles=pro

# 浏览器验证
# http://localhost:8080/swagger-ui.html
# http://localhost:8080/admin  (商业版管理后台)
```

#### Step 6: 打 tag
```bash
git add .
git commit -m "release: pro v0.1.0-pro.1"
git tag -a v0.1.0-pro.1 -m "pro v0.1.0-pro.1"
git push origin master
git push origin v0.1.0-pro.1
```

### 3.3 发布物清单（交付给客户）

```
pro-release-v0.1.0-pro.1/
├─ admin/                              ← 商业版管理后台
│  ├─ jc-rag-pro-app-admin.jar
│  └─ config/
│     ├─ application.yml
│     ├─ application-pro.yml
│     └─ LICENSE-KEY                   ← ★ 客户 License
│
├─ user/                               ← 商业版用户端
│  ├─ jc-rag-pro-app-user.jar
│  └─ config/
│     └─ LICENSE-KEY
│
├─ python/                             ← 商业版 Python
│  ├─ jc_rag_pro_service-0.1.0-pro.1.whl
│  ├─ docker/Dockerfile
│  └─ rag_config.yml
│
├─ docker-compose.yml                  ← 商业版完整编排
├─ README-pro.md                       ← 商业版部署手册
└─ LICENSE-KEY                         ← 总 License 文件
```

---

## 四、双版本同步规则

| 场景 | 同步动作 | 频率 |
|---|---|---|
| community 修复 bug | pro 同步合入 | 紧急 24h, 普通 1 周 |
| community 新功能 | pro 评估是否需要 | 按 release 周期 |
| pro 新功能 | **不同步**到 community | 商业特性独立 |
| community API 变更 | pro 必须跟进 | 同步发布 |

---

## 五、回滚流程

### 5.1 开源版回滚
```bash
git tag -d v0.1.0
git push origin :refs/tags/v0.1.0
# 通知所有 community 用户升级到 v0.1.1
```

### 5.2 商业版回滚
```bash
git tag -d v0.1.0-pro.1
git push origin :refs/tags/v0.1.0-pro.1
# 给客户发新版 jar
```

### 5.3 单个模块回滚
```bash
# 比如 jc-rag-admin 出问题, 但 jc-rag-user 正常
git revert <bad-commit>
git tag -a v0.1.1 -m "hotfix jc-rag-admin"
```

---

## 六、自动化脚本清单

| 脚本 | 位置 | 用途 |
|---|---|---|
| `extract-community.sh` | `jc-rag-pro-script/` | v1.0 拆 community 独立仓 |
| `sync-community.sh` | `jc-rag-pro-script/` | 同步 community 公开仓代码到 pro |
| `sync-pro-core.sh` | `jc-rag-pro-script/` | pro 同步 community_core（Python C 方案） |
| `build-pro.sh` | `jc-rag-pro-script/` | 商业版一键构建 |
| `build-pro.ps1` | `jc-rag-pro-script/` | 商业版一键构建（Windows） |
| `gen-license.sh` | `jc-rag-pro-script/` | 生成 License |
| `gen-license.ps1` | `jc-rag-pro-script/` | 生成 License（Windows） |
| `push-all.sh` | `jc-rag-pro-script/` | 推送商业仓到双平台 |
| `push-community-all.sh` | `jc-rag-pro-script/` | 推送 community 到双平台 |
| `setup-remotes.sh` | `jc-rag-pro-script/` | 配置多 remote |
| `release-local.ps1` | `jc-rag-pro-script/` | 本地发布到 etc 环境（保留旧版） |

---

## 七、CHANGELOG 模板

```markdown
# Changelog

## [0.2.0-pro.1] - 2026-07-15

### Added (商业版)
- 高级检索：多路召回
- 知识库评估：BLEU/ROUGE 指标

### Added (开源版)
- 知识库分析 API

### Fixed
- community: 修复向量检索空指针
- pro: 修复 License 验签时区问题

### Changed
- community: API v1 → v2, 兼容 v1
```

---

## 八、参考文档

- [项目总览](../architecture/overview.md)
- [A→C 迁移指南](../edition/a-to-c-migration.md)
- [subtree 拆分指南](../edition/subtree-guide.md)
- [双平台部署](../ops/multi-platform-deploy.md)
- [运维手册](../ops/operations.md)
