-- =============================================================================
-- 027_rbac_department_position.sql
-- -----------------------------------------------------------------------------
-- 目的: 引入部门 (rbac_department) 与岗位 (rbac_position) 两张基础表, 为系统管理提供
--       组织树 (parent_id) 与岗位级别 (level) 维度, 第二阶段 (Pro 文档级权限) 复用.
-- 业务背景:
--   权限系统增强: 现有 rbac_user / rbac_role 缺少组织维度, 无法实现"按部门 / 岗位"过滤.
--   本次先建表 + 基础 CRUD, 部门/岗位与 rbac_user 的关联 (department_id, position_id)
--   留到第二阶段 (Pro 文档级权限) 统一加, 避免本阶段改 rbac_user.
-- 表前缀: rbac_ (与 rbac_user / rbac_role / rbac_permission 一致, 属于权限域表)
-- 兼容: 全新建表, 不动现有表, 不需要 ALTER.
-- =============================================================================

CREATE TABLE IF NOT EXISTS rbac_department (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0      COMMENT '父部门 id, 0=顶级',
    name        VARCHAR(50)  NOT NULL                 COMMENT '部门名称',
    code        VARCHAR(50)  NOT NULL                 COMMENT '部门编码 (业务引用, 唯一)',
    sort        INT          NOT NULL DEFAULT 0       COMMENT '同级排序, 升序',
    status      TINYINT      NOT NULL DEFAULT 1       COMMENT '0=禁用 1=启用',
    remark      VARCHAR(255) DEFAULT NULL             COMMENT '备注',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rbac_department_code (code),
    KEY idx_rbac_department_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表 (RBAC 组织维度)';

CREATE TABLE IF NOT EXISTS rbac_position (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    department_id BIGINT       NOT NULL                COMMENT '所属部门 id',
    name          VARCHAR(50)  NOT NULL                COMMENT '岗位名称',
    code          VARCHAR(50)  NOT NULL                COMMENT '岗位编码 (业务引用, 唯一)',
    level         TINYINT      NOT NULL DEFAULT 1      COMMENT '岗位级别: 1=初级 2=中级 3=高级',
    sort          INT          NOT NULL DEFAULT 0      COMMENT '同级排序, 升序',
    status        TINYINT      NOT NULL DEFAULT 1      COMMENT '0=禁用 1=启用',
    remark        VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rbac_position_code (code),
    KEY idx_rbac_position_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表 (RBAC 组织维度)';
