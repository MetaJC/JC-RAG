-- =============================================================================
-- 035_rbac_user_position.sql
-- -----------------------------------------------------------------------------
-- 目的: 引入用户-岗位 多对多关联表 rbac_user_position.
--   之前 rbac_user 只有 rbac_user_department (用户-部门), 没有岗位关联.
--   岗位与部门解耦后, 用户岗位归属需要独立存储.
-- 业务背景:
--   权限系统增强: 一个用户可兼任多个岗位 (如: 主岗=产品经理, 兼职=运营专员).
--   岗位与部门解耦: 不再通过 rbac_position.department_id 限定岗位范围.
-- 表前缀: rbac_ (与 rbac_user / rbac_position 一致).
-- 兼容: 全新建表, 不动现有表, 不需要 ALTER.
-- =============================================================================

CREATE TABLE IF NOT EXISTS rbac_user_position (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL                COMMENT '用户 id',
    position_id BIGINT       NOT NULL                COMMENT '岗位 id',
    is_primary  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否主岗位 0=否 1=是',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rbac_user_position (user_id, position_id),
    KEY idx_rbac_user_position_user (user_id),
    KEY idx_rbac_user_position_position (position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-岗位关联表 (多对多)';
