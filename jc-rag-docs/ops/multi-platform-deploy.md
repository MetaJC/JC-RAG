# gitee + github 双平台部署指南

> 适用场景: 商业仓 + 公开仓在 gitee 和 github 同时维护.

## 平台对比

| 平台 | 优势 | 劣势 | 国内访问 | 推荐用途 |
|---|---|---|---|---|
| **gitee** | 国内速度快, 中文社区 | 国际化社区较弱 | ✅ 极快 | 国内用户为主, 商业版优先 |
| **github** | 全球生态, CI 工具链最全 | 国内访问慢 | ⚠️ 不稳定 | 开源社区, 国际用户 |

## 双平台架构

```
        ┌────────────────────────────┐
        │  商业版仓库                 │
        ├────────────────────────────┤
        │  gitee: jc-rag.git (私有)  │  ← 国内, 主要开发
        │  github: jc-rag.git (私有) │  ← 镜像, 海外备份
        └────────────────────────────┘
                    ▲
                    │ 商业仓 push
                    │
        ┌────────────────────────────┐
        │  开源版仓库                 │
        ├────────────────────────────┤
        │  gitee:  jc-rag-community.git (公开) │  ← 国内社区
        │  github: jc-rag-community.git (公开) │  ← 国际社区
        └────────────────────────────┘
                    ▲
                    │ subtree push
                    │
        ┌────────────────────────────┐
        │  本地工作目录               │
        │  d:\Project\jc-rag\         │
        └────────────────────────────┘
```

**总计 4 个仓库**:

| 仓库 | 平台 | 可见性 |
|---|---|---|
| jc-rag.git | gitee | 私有 |
| jc-rag.git | github | 私有 |
| jc-rag-community.git | gitee | 公开 |
| jc-rag-community.git | github | 公开 |

## 一次性配置 (首次)

### 1. 在两个平台分别建仓

**gitee** (假设已完成):
- jc-rag (私有)
- jc-rag-community (公开)

**github**:
- 登录 github → New repository
- Name: `jc-rag`
- Visibility: Private
- 不要勾选 Add README
- Create repository
- 同样方式创建 `jc-rag-community` (Public)

### 2. 配置商业仓的 4 个 remote

在 `d:\Project\jc-rag` 根目录:

```bash
# 商业仓 remote
git remote add gitee-origin    https://gitee.com/Open-JC/jc-rag.git
git remote add github-origin   https://github.com/Open-JC/jc-rag.git

# 公开仓 remote (subtree 拆分后才有)
git remote add gitee-community  https://gitee.com/Open-JC/jc-rag-community.git
git remote add github-community https://github.com/Open-JC/jc-rag-community.git

# 验证
git remote -v
# 期望:
# gitee-origin       https://gitee.com/Open-JC/jc-rag.git
# github-origin      https://github.com/Open-JC/jc-rag.git
# gitee-community    https://gitee.com/Open-JC/jc-rag-community.git
# github-community   https://github.com/Open-JC/jc-rag-community.git
```

### 3. 简化命令: 一键推双平台

双平台推送脚本已落地为 `jc-rag-pro-script/push-pro-repo.sh`（参考实现如下）:

```bash
#!/bin/bash
# 同时推送到 gitee + github
set -e

BRANCH="${1:-master}"
echo "=== 推送到所有平台 ($BRANCH) ==="

for remote in gitee-origin github-origin; do
    echo "--- $remote ---"
    git push "$remote" "$BRANCH"
done

echo "✅ 商业仓双平台推送完成"
```

```bash
chmod +x jc-rag-pro-script/push-pro-repo.sh
./jc-rag-pro-script/push-pro-repo.sh
```

公开仓双平台推送对应 `jc-rag-pro-script/push-community-repo.sh` (参考实现):

```bash
#!/bin/bash
set -e
BRANCH="${1:-master}"
PREFIX="community"
echo "=== 推 community/ 到双平台公开仓 ==="

for remote in gitee-community github-community; do
    echo "--- $remote ---"
    git subtree push --prefix="$PREFIX" "$remote" "$BRANCH"
done
echo "✅ 公开仓双平台推送完成"
```

## 日常使用

### 场景 A: 改了商业代码, 推双平台

```bash
# 1. 提交
git add pro enterprise
git commit -m "feat: 新增商业版功能"
./jc-rag-pro-script/push-pro-repo.sh master
# 推送: gitee-origin + github-origin
```

### 场景 B: 改了 community, 推双平台公开仓

```bash
# 1. 提交
git add community
git commit -m "feat: community 新增 KB 视图"

# 2. 推商业仓
git push gitee-origin master
git push github-origin master

# 3. 推公开仓双平台
./jc-rag-pro-script/push-community-repo.sh master
# 推送: gitee-community + github-community
```

### 场景 C: 公开仓有 PR 合并, 拉本地

```bash
# PR 可能来自 gitee-community 或 github-community
# 配置 fetch all remotes
git fetch --all

# 检查哪个平台有新 commit
git log gitee-community/master..HEAD --oneline  # gitee 领先?
git log github-community/master..HEAD --oneline  # github 领先?

# 选一个拉取
git subtree pull --prefix=community gitee-community master --squash
# 或
git subtree pull --prefix=community github-community master --squash
```

## 双平台一致性问题

### 问题 1: 误推不同内容

**原因**: 本地 commit 在 push 之前两个平台是独立的, 推完后内容一致. 但**操作失误**可能导致某平台少了 commit.

**解决**: 写检查脚本 `scripts/check-sync.sh`:

```bash
#!/bin/bash
# 检查双平台同步状态
BRANCH="${1:-master}"
echo "=== 检查 4 个仓库同步状态 ==="

# 商业仓
echo "[商业仓]"
for r in gitee-origin github-origin; do
    LOCAL=$(git rev-parse "HEAD")
    REMOTE=$(git rev-parse "$r/$BRANCH" 2>/dev/null || echo "未连接")
    if [ "$LOCAL" = "$REMOTE" ]; then
        echo "  ✅ $r: 同步 (${LOCAL:0:7})"
    else
        echo "  ❌ $r: 不同步 (local: ${LOCAL:0:7}, remote: ${REMOTE:0:7})"
    fi
done

# 公开仓
echo "[公开仓]"
for r in gitee-community github-community; do
    REMOTE=$(git rev-parse "$r/$BRANCH" 2>/dev/null || echo "未连接")
    echo "  $r: ${REMOTE:0:7}"
done
```

### 问题 2: PR 处理

**gitee 提了 PR #10, github 也有 #10, 哪个为准?**

**建议**:
- **主平台 = gitee** (国内开发优先)
- github 是**镜像**, PR 一般关闭 ("请到 gitee 提 PR")
- 或在 README 写明: "请到 gitee 提 PR, github 仅镜像"

### 问题 3: CI/CD 配置

**gitee Go** + **GitHub Actions** 双套:

| 文件 | 作用 |
|---|---|
| `.gitee-ci.yml` | gitee CI 配置 (国内速度快) |
| `.github/workflows/*.yml` | GitHub Actions 配置 (海外生态) |

**双 CI 文件同步维护成本**: 中等. 建议:
- 简单项目: 只配 gitee CI
- 国际化项目: 双 CI (gitee 跑 build, github 跑 release)

## 备份策略

### 自动备份 (cron / 任务计划)

```bash
# 每天凌晨 2 点全量备份到本地
0 2 * * * cd /path/to/jc-rag && git push --all --mirror /backup/jc-rag-$(date +\%Y\%m\%d).git
```

### 备份到另一台机器

```bash
# 用 ssh 推送到备份服务器
git remote add backup ssh://backup-server/~/jc-rag-backup.git
git push --all --mirror backup
```

## 平台选择建议

### 仅选 gitee (推荐起步)
- 项目初期, 国内用户为主
- 商业版优先, 开源版同步
- 简单 (2 个仓库)

### 仅选 github
- 国际用户为主
- 重视生态 (GitHub Actions, GitHub Pages)
- 简单 (2 个仓库)

### 双平台 (gitee + github)
- 国内 + 国际都要覆盖
- **配置成本**: 4 个 remote, 4 个仓库
- **维护成本**: 推 commit 时 2 倍, PR 2 套
- **适合**: 中后期, 用户量大

### 不推荐
- **gitee + gitlab + github**: 3 平台成本太高, 不在 v1.0 考虑

## 相关脚本

| 脚本 | 用途 |
|---|---|
| `jc-rag-pro-script/push-pro-repo.sh` | 推商业仓到双平台 |
| `jc-rag-pro-script/push-community-repo.sh` | 推公开仓到双平台 |
| `jc-rag-pro-script/sync-community-subtree.sh` | subtree push/pull/status |
| `jc-rag-pro-script/setup-git-remotes.sh` | 配置多 remote |
