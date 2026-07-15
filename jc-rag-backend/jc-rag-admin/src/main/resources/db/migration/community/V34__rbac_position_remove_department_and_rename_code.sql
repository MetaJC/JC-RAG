-- =============================================================================
-- 034_rbac_position_remove_department_and_rename_code.sql
-- -----------------------------------------------------------------------------
-- 目的: 岗位与部门解耦
--   1) 删除 rbac_position.department_id (岗位不再绑定单一部门)
--   2) rbac_position.code 重命名为 display_name (岗位编码无具体业务作用,
--      改为显示名, 如: 岗位名=人力资源, 显示名=HR)
--   3) 删除 uk_rbac_position_code 唯一索引 (display_name 不再要求唯一)
--   4) 删除 idx_rbac_position_department 索引 (已无 department_id)
-- 业务背景:
--   之前 rbac_position 关联 rbac_department, 但实际岗位与用户-部门关联无直接关系
--   (用户-部门走 rbac_user_department), 移除冗余外键字段; 同时 code 字段无业务引用,
--   改为更常用的 display_name, 不再唯一.
-- 兼容:
--   若 rbac_position 已有数据且 display_name (原 code) 重复, 需要手动去重后再升级.
--   本脚本不做数据迁移 (历史库数据由 DBA 评估后处理), 后续部署前请先确认无重复 code.
-- =============================================================================

-- 1) 删除唯一索引和外键索引
ALTER TABLE rbac_position DROP INDEX uk_rbac_position_code;
ALTER TABLE rbac_position DROP INDEX idx_rbac_position_department;

-- 2) 删除 department_id 列
ALTER TABLE rbac_position DROP COLUMN department_id;

-- 3) code → display_name
ALTER TABLE rbac_position
    CHANGE COLUMN code display_name VARCHAR(50) NOT NULL
    COMMENT '显示名 (简称 / 英文名, 例如: 岗位名=人力资源, 显示名=HR)';
