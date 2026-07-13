# 数据库表结构 (RBAC 权限域)

> 本文档描述 RBAC (Role-Based Access Control) 权限域的所有数据表, 涵盖用户 / 角色 / 权限 / 菜单 / 部门 / 岗位.
> 完整数据库结构请参考 Flyway 迁移脚本: [flyway.md](./flyway.md).

## 1. RBAC 权限域表清单

| 表名 | 中文名 | 引入版本 | 说明 |
|---|---|---|---|
| `rbac_user` | 用户表 | V1 | 管理员/操作员账号 |
| `rbac_role` | 角色表 | V1 | 角色 + is_admin 标志 |
| `rbac_permission` | 权限表 | V1 | 菜单/API/按钮三类权限点 |
| `rbac_role_permission` | 角色-权限关联表 | V1 | 含 data_scope 字段 |
| `rbac_user_role` | 用户-角色关联表 | V1 | 多对多 |
| `rbac_department` | 部门表 | V27 | 组织树 (parent_id 自引用) |
| `rbac_position` | 岗位表 | V27 | 隶属部门, 含级别 |

> 旧 `sys_*` 表 (`sys_role` / `sys_permission` / `sys_role_permission`) 已废弃, 新功能不再读写.

## 2. rbac_department 部门表

引入版本: **V27** (2026-07-13)
SQL: [V27__rbac_department_position.sql](../../jc-rag-community/jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/V27__rbac_department_position.sql)

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint | ✅ | AUTO | 主键 |
| parent_id | bigint | ✅ | 0 | 父部门 id, 0=顶级 |
| name | varchar(50) | ✅ | | 部门名称 |
| code | varchar(50) | ✅ | | 部门编码 (业务引用, 唯一) |
| sort | int | ✅ | 0 | 同级排序, 升序 |
| status | tinyint | ✅ | 1 | 0=禁用 1=启用 |
| remark | varchar(255) | | | 备注 |
| created_at | datetime | ✅ | CURRENT_TIMESTAMP | |
| updated_at | datetime | ✅ | CURRENT_TIMESTAMP ON UPDATE | |

| 索引 | 类型 | 字段 |
|---|---|---|
| PRIMARY | PK | id |
| uk_rbac_department_code | UNIQUE | code |
| idx_rbac_department_parent | IDX | parent_id |

### 业务规则
- 顶级部门: `parent_id = 0` (而不是 NULL)
- 删除校验: 有子部门 / 有启用岗位时禁止删除
- 业务编码 `code` 全表唯一, 修改编码需校验唯一性

## 3. rbac_position 岗位表

引入版本: **V27** (2026-07-13)

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint | ✅ | AUTO | 主键 |
| department_id | bigint | ✅ | | 所属部门 id |
| name | varchar(50) | ✅ | | 岗位名称 |
| code | varchar(50) | ✅ | | 岗位编码 (业务引用, 唯一) |
| level | tinyint | ✅ | 1 | 岗位级别: 1=初级 2=中级 3=高级 |
| sort | int | ✅ | 0 | 同级排序, 升序 |
| status | tinyint | ✅ | 1 | 0=禁用 1=启用 |
| remark | varchar(255) | | | 备注 |
| created_at | datetime | ✅ | CURRENT_TIMESTAMP | |
| updated_at | datetime | ✅ | CURRENT_TIMESTAMP ON UPDATE | |

| 索引 | 类型 | 字段 |
|---|---|---|
| PRIMARY | PK | id |
| uk_rbac_position_code | UNIQUE | code |
| idx_rbac_position_department | IDX | department_id |

### 业务规则
- 删除部门时若存在 `status=1` 的岗位, 禁止删除
- 岗位级别枚举值: 1/2/3, 不在枚举内视为非法 (前端下拉框控制)

## 4. 部门/岗位新增菜单与权限点

引入版本: **V28** (2026-07-13)
SQL: [V28__rbac_department_position_menu.sql](../../jc-rag-community/jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/V28__rbac_department_position_menu.sql)

### 4.1 新增菜单 (parent = rbac:manage)
| perm_code | 名称 | 路径 | 图标 | 排序 |
|---|---|---|---|---|
| rbac:departments | 部门管理 | /rbac/departments | OfficeBuilding | 50 |
| rbac:positions | 岗位管理 | /rbac/positions | UserFilled | 60 |

### 4.2 新增按钮权限
| perm_code | 名称 | 父菜单 |
|---|---|---|
| rbac:department:create | 新增部门 | rbac:departments |
| rbac:department:update | 编辑部门 | rbac:departments |
| rbac:department:delete | 删除部门 | rbac:departments |
| rbac:department:status | 切换部门状态 | rbac:departments |
| rbac:position:create | 新增岗位 | rbac:positions |
| rbac:position:update | 编辑岗位 | rbac:positions |
| rbac:position:delete | 删除岗位 | rbac:positions |
| rbac:position:status | 切换岗位状态 | rbac:positions |
| rbac:department:move | 拖拽调整部门结构 | rbac:departments |

### 4.3 角色绑定
| 角色 | 权限范围 |
|---|---|
| super_admin | 全部 10 个新权限 (菜单 + 全部按钮) |
| admin | 2 个菜单权限 (只读视图, 无按钮) |

### 4.4 部门拖拽 move 接口

`PUT /api/rbac/department/{id}/move`

请求体:

```json
{
  "targetId": 12,
  "position": "before"  // before | after | inner
}
```

语义:
- `before`: 变成 `targetId` 的同级, 排在 `targetId` 之前
- `after` : 变成 `targetId` 的同级, 排在 `targetId` 之后
- `inner` : 变成 `targetId` 的子级, 排在同级最后 (sort = max+1)

后端约束:
- `id == targetId` → 拒绝
- `targetId` 是 `id` 的后代 (含深层) → 拒绝 (防环, MySQL 8 递归 CTE)
- 旧父级兄弟在跨父级移动时 sort 重新连续化, 避免留空洞
- 新父级兄弟 sort 全部 +1 腾位

## 5. 与 rbac_user 关联 (未来扩展)

第二阶段 (Pro 文档级权限) 会在 `rbac_user` 表增加:
- `department_id BIGINT` — 用户所属部门
- `position_id BIGINT` — 用户岗位

目前 `rbac_user` 不含部门/岗位字段, 与本表无外键约束, 避免本阶段改动 user 表.

## 6. 维护说明

- 表结构变更: 修改 `db/migration/community/V<新号>__<描述>.sql`, 不要直接 ALTER 库
- 文档同步: 任何字段增减必须同步更新本文件
- 双版本同步: 详见 [flyway.md](./flyway.md) 第 3 节
