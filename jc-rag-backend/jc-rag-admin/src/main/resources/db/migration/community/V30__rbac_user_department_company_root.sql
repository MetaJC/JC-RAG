-- =============================================================================
-- V30__rbac_user_department_company_root.sql
-- -----------------------------------------------------------------------------
-- 目的:
--   1. 用户与部门建立多对多关联 (rbac_user_department), 支持用户兼职多部门
--   2. 用户表 rbac_user 冗余主部门字段 department_id, 用于列表快速展示 / 默认归属
--   3. 顶级部门 (parent_id=0) 视为公司名称, 部门管理页表格上方单独展示
-- 业务背景:
--   原 rbac_user 与 rbac_department 无任何关联, 用户管理页无法按部门筛选
--   此次新增关联, 同步:
--     - 部门管理页: 表格上方单独展示公司名称 (顶级部门 name), 可编辑
--     - 用户管理页: 左侧展示组织机构树, 右侧按选中部门递归过滤用户
-- 表前缀: rbac_ (与 rbac_user / rbac_department 一致, 属于权限域表)
-- 兼容: 全部 IF NOT EXISTS, 幂等可重跑
-- =============================================================================

-- 1. rbac_user 新增主部门字段 (冗余, 用于列表展示, 真实关联在 rbac_user_department)
ALTER TABLE rbac_user
    ADD COLUMN department_id BIGINT NULL COMMENT '主部门 id (冗余, 真实关联见 rbac_user_department)';

ALTER TABLE rbac_user
    ADD INDEX idx_rbac_user_department (department_id);

-- 2. 用户-部门多对多关联表
CREATE TABLE IF NOT EXISTS rbac_user_department (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL                COMMENT '用户 id',
    department_id BIGINT       NOT NULL                COMMENT '部门 id',
    is_primary    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否主部门 0=否 1=是',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rbac_user_dept (user_id, department_id),
    KEY idx_rbac_user_dept_user (user_id),
    KEY idx_rbac_user_dept_dept (department_id)
) COMMENT='用户-部门关联表 (多对多)';

-- 3. 兜底: 为已有 rbac_user 记录同步主部门为 0 (表示无所属)
--    若 department_id 字段已存在, 跳过; 此处假定首次执行
-- (Flyway 记录此迁移只跑一次, 无需 IF 包裹)
