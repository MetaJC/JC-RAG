# v1.0 拆分到独立公开仓 - 完整迁移指南

> 适用阶段: 项目从单仓过渡到双仓 (商业仓 + 公开仓). 一次性操作, 不可逆, 务必按顺序执行.

## 拆分前的准备

### 1. 单仓已稳定

确保以下条件全部满足:

- [x] 当前单仓 (`gitee.com/Open-JC/jc-rag.git`) 已稳定运行
- [x] 商业版构建 (`build-pro.sh`) 通过
- [x] 开源版构建 (`mvn -pl jc-rag-community/jc-rag-backend -am package`) 通过
- [x] 所有敏感文件已加 `.gitignore` (已验证)
- [x] 工作区干净 (`git status` 无变更)

### 2. 在 gitee 创建公开仓

```
1. 登录 gitee → 右上角 "+" → 新建仓库
2. 仓库名: jc-rag-community
3. 路径: jc-rag-community (或你想要的)
4. 可见性: 公开 (Public)
5. 仓库介绍: "JC RAG 开源版 - AI 知识库 RAG 系统"
6. 初始化: 不要勾选任何一项 (README/.gitignore/许可证 全部不勾)
7. 直接创建 (保持空仓)
```

### 3. 备份 (强烈建议)

```bash
# 备份整个 d:\Project\jc-rag 到别处
xcopy D:\Project\jc-rag D:\Project\jc-rag-backup-2026xxxx /E /H /I
```

## 拆分流程

### Step 1: 配置 community remote

在 `d:\Project\jc-rag` 根目录执行:

```bash
# 方式 1: 跑脚本 (推荐)
bash jc-rag-pro-script/setup-git-remotes.sh
# 按提示选 y 添加 community remote

# 方式 2: 手动
git remote add community https://gitee.com/Open-JC/jc-rag-community.git
git remote -v
# 期望看到两个 remote: origin (商业) + community (公开)
```

### Step 2: 提取 community 历史

```bash
# 跑 split-community-repo.sh
bash jc-rag-pro-script/split-community-repo.sh

# 脚本会自动:
# 1. 验证工作区干净
# 2. git subtree split --prefix=community -b community-standalone
# 3. 询问确认后, 推送到公开仓 master
# 4. 删除临时分支
```

### Step 3: 验证公开仓

```bash
# 在临时目录 clone 公开仓
git clone https://gitee.com/Open-JC/jc-rag-community.git /tmp/test-community

# 检查文件
ls /tmp/test-community
# 预期: backend-java/  frontend/  docs/  python/  README.md ...

# 验证大小 (应 < 5MB, 不含 target/node_modules)
du -sh /tmp/test-community
```

### Step 4: 第一次同步测试

```bash
# 在 d:\Project\jc-rag 商业仓跑
bash jc-rag-pro-script/sync-community-subtree.sh status
# 应显示: 商业仓/公开仓 双向差异 (无)

# 测试 push (改个注释类文件, 不影响功能)
echo "# 同步测试注释" >> jc-rag-community/jc-rag-docs/README.md
git add jc-rag-community/jc-rag-docs/README.md
git commit -m "test: 验证 subtree push 链路"
bash jc-rag-pro-script/sync-community-subtree.sh push
# 应成功推送到公开仓
```

### Step 5: 回滚 (如果需要)

如果中途出问题, 完整回滚:

```bash
# 1. 删除公开仓 (在 gitee 网页操作, 仓库 → 设置 → 删除)
# 2. 商业仓删除 community remote
cd d:\Project\jc-rag
git remote remove community
# 3. 删除提取历史时残留的临时分支 (如果有)
git branch -D community-standalone
# 4. 商业仓回到单仓状态, 无任何副作用
```

## 拆分后日常操作

### 改 jc-rag-community/ → 推公开仓

```bash
# 1. 改文件
vim jc-rag-community/jc-rag-backend/.../XxxController.java

# 2. 提交
git add jc-rag
git commit -m "feat: 新增 XX 功能"

# 3. 推商业仓
git push origin master

# 4. 推公开仓 (选择性, 商业定制不推)
bash jc-rag-pro-script/sync-community-subtree.sh push
```

### 公开仓有 PR 合并 → 拉到商业仓

```bash
# 公开仓 PR #42 已合到 master
bash jc-rag-pro-script/sync-community-subtree.sh pull
# 商业仓 jc-rag-community/ 自动同步, 一次 commit 记录

git push origin master
```

### 紧急: 误推商业代码到公开仓

```bash
# 1. 在公开仓 (临时 clone 一份)
git clone https://gitee.com/Open-JC/jc-rag-community.git /tmp/fix-community
cd /tmp/fix-community

# 2. 找到误推 commit
git log --oneline | head -20

# 3. revert
git revert <commit-hash>
git push origin master

# 4. 商业仓同步
cd d:\Project\jc-rag
bash jc-rag-pro-script/sync-community-subtree.sh pull
```

## 拆分后仓库结构对比

| 维度 | 商业仓 (origin) | 公开仓 (community) |
|---|---|---|
| **URL** | gitee.com/Open-JC/jc-rag.git | gitee.com/Open-JC/jc-rag-community.git |
| **可见性** | 私有 | 公开 |
| **谁可以访问** | 仅你 | 任何人 (clone/fork/PR) |
| **包含内容** | jc-rag-pro-core/ + jc-rag-pro-enterprise/ + jc-rag-community/ + jc-rag-pro-script/ + LICENSE-KEY | jc-rag-community/ (纯开源) |
| **commit history** | 完整 (含商业 commit) | 从拆分点开始的社区 commit |
| **LICENSE** | 商业 License | MIT 或 Apache 2.0 (你选) |

## 公开仓推荐配置 (在 gitee 网页设置)

| 设置项 | 推荐值 |
|---|---|
| **默认分支** | master |
| **保护分支** | 启用 master 保护 (不允许 force push) |
| **Issue 模板** | Bug Report / Feature Request |
| **PR 模板** | 描述 / 改动文件 / 测试用例 |
| **Wiki** | 启用 (放开发文档) |
| **License** | MIT (宽松) 或 Apache 2.0 (专利友好) |
| **README 徽章** | gitee stars / fork / build status |

## 相关脚本

| 脚本 | 用途 |
|---|---|
| `jc-rag-pro-script/split-community-repo.sh` | 本次拆分的主脚本 |
| `jc-rag-pro-script/sync-community-subtree.sh` | 拆分后日常 push/pull |
| `jc-rag-pro-script/setup-git-remotes.sh` | 配置双 remote |
| `jc-rag-pro-script/build-pro.sh` | 商业版构建 (不涉及仓库) |
