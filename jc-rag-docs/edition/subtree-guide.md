# 双仓库 Subtree 拆分指南

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  商业版私有仓 (origin)                                        │
│  gitee.com/Open-JC/jc-rag.git                                │
│  ┌──────────┬──────────┬──────────┬──────────┐               │
│  │jc-rag-community/ │jc-rag-pro-core/ │jc-rag-pro-enterprise/ │jc-rag-pro-script/│               │
│  └──────────┴──────────┴──────────┴──────────┘               │
│       ▲                                                       │
│       │ git subtree push/pull                                 │
└───────┼──────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  开源公开仓 (community)                                       │
│  gitee.com/Open-JC/jc-rag-community.git                       │
│  完整 GitHub 功能: clone / fork / PR / issues                 │
│  任何人可独立访问                                              │
└─────────────────────────────────────────────────────────────┘
```

## 当前状态（v0.x 阶段）

**单仓结构**：所有代码（含 jc-rag-community/）都在 `gitee.com/Open-JC/jc-rag.git` 一个仓库里。

`.gitmodules` 已删除（单仓结构不需要）。

## v1.0 拆分流程（届时执行）

### 1. 准备工作

```bash
# 1.1 在 gitee 新建 jc-rag-community 公开仓
#     - 登录 gitee → 新建仓库
#     - 仓库名: jc-rag-community
#     - 可见性: 公开
#     - 不要勾选 "使用 README 文件初始化"
#     - 不要勾选 "添加 .gitignore"
#     - 不要勾选 "添加许可证"
#     - 直接创建（保持空仓）

# 1.2 商业仓工作区干净
cd d:\Project\jc-rag
git status  # 确保无未提交变更
```

### 2. 执行拆分

```bash
# 2.1 跑 extract-community.sh 脚本
bash scripts/extract-community.sh

# 2.2 验证公开仓
git clone https://gitee.com/Open-JC/jc-rag-community.git /tmp/test-community
ls /tmp/test-community
# 预期看到: jc-rag-frontend, jc-rag-backend, jc-rag-docs ...
```

### 3. 配置 community remote

```bash
bash scripts/setup-remotes.sh
# 选 y 添加 community remote
```

## 日常 subtree 操作

### 场景 A：商业仓改了 jc-rag-community/，要推回公开仓

```bash
# 1. 改文件
vim jc-rag-community/jc-rag-frontend/src/views/KbView.vue

# 2. 提交
git add jc-rag
git commit -m "fix: 修复 KB 视图渲染 bug"

# 3. 推商业仓
git push origin master

# 4. 推公开仓（让社区也能用这个修复）
bash jc-rag-pro-script/sync-community.sh push
```

### 场景 B：公开仓有 PR 合并了，拉到商业仓

```bash
# 1. 拉公开仓更新
bash jc-rag-pro-script/sync-community.sh pull

# 2. 验证 (检查 jc-rag-community/ 文件变更)
git diff HEAD~1 jc-rag-community/

# 3. 推商业仓
git push origin master
```

### 场景 C：商业仓内部演进，不反馈给公开仓

```bash
# 改 jc-rag-community/ 里的文件 (例如商业定制)
vim jc-rag-community/jc-rag-backend/.../KbProController.java

# 提交 + 推送 (默认只推商业仓)
git add jc-rag
git commit -m "feat: jc-rag 模块商业定制"
git push origin master

# ⚠️ 不要跑 sync-community.sh push, 公开仓永远不知道这个修改
```

## 切换到 submodule（如果将来需要）

```bash
# 1. 删除 jc-rag-community/ 在商业仓的跟踪
cd d:\Project\jc-rag
git rm -rf jc-rag
git commit -m "refactor: remove jc-rag-community/, prepare for submodule"

# 2. 添加 submodule
git submodule add https://gitee.com/Open-JC/jc-rag-community.git jc-rag
git commit -m "feat: add jc-rag-community as submodule"

# 3. 推商业仓
git push origin master
```

切回 subtree：

```bash
# 1. 移除 submodule
git submodule deinit jc-rag
git rm jc-rag
git commit -m "refactor: convert from submodule to subtree"

# 2. 从公开仓拉取 (会自动建 jc-rag-community/ 目录)
git remote add community https://gitee.com/Open-JC/jc-rag-community.git
git subtree add --prefix=jc-rag community master --squash

# 3. 推
git push origin master
```

## 常见问题

### Q1: community remote 没配怎么办？
跑 `bash scripts/setup-remotes.sh`，会引导你添加。

### Q2: 推送冲突怎么办？
公开仓和商业仓可能各自演进。解决：
```bash
# 拉公开仓到商业仓 (会合并)
bash scripts/sync-community.sh pull

# 解决可能的冲突
# 然后再 push 商业仓
```

### Q3: 误推商业代码到公开仓？
**回滚**：
1. 公开仓 git log 找到误推 commit
2. 公开仓 git revert <commit-hash>
3. 公开仓 git push
4. 商业仓同步：bash jc-rag-pro-script/sync-community.sh pull

### Q4: 公开仓被独立 clone 时，jc-rag-community/ 在哪？
- **subtree 模式**：clone 后 jc-rag-community/ 就在本地，是普通目录
- **submodule 模式**：clone 后 jc-rag-community/ 是空目录，需 `git submodule update`

## 相关脚本

| 脚本 | 用途 |
|---|---|
| `jc-rag-pro-script/extract-community.sh` | v1.0 一次性提取 jc-rag-community/ 到独立公开仓 |
| `jc-rag-pro-script/sync-community.sh` | 日常双向同步（push/pull/status） |
| `jc-rag-pro-script/setup-remotes.sh` | 配置双 remote |
| `jc-rag-pro-script/build-pro.sh` | 构建商业版（不涉及仓库） |
