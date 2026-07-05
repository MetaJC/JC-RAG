# 测试账号 (Test Users)

> Agent 测试专用账号, 与生产账号隔离. 所有测试账号 `username` 前缀 `test_`.
> 密码统一 `Test@123456` (仅本地 dev 环境).

## 账号清单

| username | password | role | is_admin | user_type | 说明 |
|----------|----------|------|----------|-----------|------|
| test_user_01 | Test@123456 | NORMAL | 0 | NORMAL | 普通 C 端用户, 用于 chat / 记忆 / 偏好 |
| test_user_02 | Test@123456 | NORMAL | 0 | NORMAL | 同上, 用于多用户场景 (如 chat 跨用户隔离) |
| test_user_03 | Test@123456 | NORMAL | 0 | NORMAL | 同上, 用于跨会话记忆 |
| test_admin_01 | Test@123456 | ADMIN | 1 | ADMIN | 管理员, 用于 RBAC / KB 管理 |
| test_super_admin | Test@123456 | SUPER_ADMIN | 1 | ADMIN | 超级管理员, 用于最高权限场景 |
| test_developer | Test@123456 | DEVELOPER | 1 | ADMIN | 开发者, 跳过所有权限校验 (项目规约) |

## 准备方式 (一次性)

> Agent 启动时若发现账号不存在, 自动调 `POST /api/auth/register` 创建, 然后用 `POST /api/auth/login` 登录拿 session.

### 手动准备 (开发期)

```sql
-- 1. 创建 6 个测试账号 (password 用 BCrypt 加密, 值为 'Test@123456')
-- 2. 关联角色 (test_user_* 关联 NORMAL role, test_admin 关联 ADMIN, ...)
-- 3. 关联默认知识库
-- (具体 SQL 见 scripts/seed_test_users.sql, 本轮不写, Agent 自动创建)
```

## 用法

### 引用账号

在 `test-cases.md` 中:
```yaml
- **PreConditions**:
  - 用户 test_user_01 已登录 (Cookie: JC_SESSION=...)
```

### 跨用户场景

```yaml
- **Steps**:
  1. test_user_01 发送消息 "A"
  2. test_user_02 发送消息 "B"  (不同 session)
  3. 验证 test_user_01 的 L0 history 不包含 "B"  (隔离)
```

## 注意事项

- **不要** 用 `test_user_*` 账号做生产演示
- **不要** 给测试账号设真实邮箱 / 手机号 (用 `test_user_01@local.test`)
- **不要** 在 result 文件中明文存 session cookie, 脱敏为 `${sessionCookie}`
- 测试完成后, Agent 自动清理本 feature 产生的 chat_message / L2 facts (见 feature.md PostConditions)
