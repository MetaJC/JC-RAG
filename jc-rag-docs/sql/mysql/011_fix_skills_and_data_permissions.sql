-- =====================================================================
-- 011: 修复 data_permissions 表缺失 + sys_skills 缺字段
-- <p>症状:
--   <ul>
--     <li>GET /api/admin/data-permissions → Table 'jc_rag.data_permissions' doesn't exist</li>
--     <li>GET /api/admin/skills → Unknown column 'trigger_rules' in 'field list'</li>
--   </ul>
-- <p>原因: 历史数据库残留旧版 sys_skills, 缺 trigger_rules / created_by
-- <p>002_modules.sql 是 CREATE TABLE IF NOT EXISTS, 不会自动加列
-- <p>MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS, 用存储过程判断列是否存在
--
-- @author JC
-- =====================================================================

DROP PROCEDURE IF EXISTS fix_sys_skills_columns;
DELIMITER //
CREATE PROCEDURE fix_sys_skills_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'jc_rag' AND TABLE_NAME = 'sys_skills' AND COLUMN_NAME = 'trigger_rules'
  ) THEN
    ALTER TABLE `sys_skills` ADD COLUMN `trigger_rules` VARCHAR(500) NULL COMMENT '触发规则（JSON格式，如关键词列表）' AFTER `trigger_type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'jc_rag' AND TABLE_NAME = 'sys_skills' AND COLUMN_NAME = 'created_by'
  ) THEN
    ALTER TABLE `sys_skills` ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人' AFTER `status`;
  END IF;
END //
DELIMITER ;

CALL fix_sys_skills_columns();
DROP PROCEDURE fix_sys_skills_columns;

-- 创 data_permissions 表 (IF NOT EXISTS, 已建则跳过)
CREATE TABLE IF NOT EXISTS `data_permissions` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `role_id`         BIGINT          NOT NULL                 COMMENT '角色ID',
    `resource_type`   VARCHAR(50)     NOT NULL                 COMMENT '资源类型: knowledge_base=知识库, document=文档, field=字段',
    `resource_id`     BIGINT          NOT NULL                 COMMENT '资源ID',
    `permission`      VARCHAR(50)     NOT NULL DEFAULT 'read'  COMMENT '权限: read=可读, deny=拒绝',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_resource` (`role_id`, `resource_type`, `resource_id`),
    KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据级权限表';

-- 验证
SELECT 'sys_skills columns' AS section;
SHOW COLUMNS FROM `sys_skills` LIKE 'trigger_rules';
SHOW COLUMNS FROM `sys_skills` LIKE 'created_by';

SELECT 'data_permissions' AS section;
SHOW CREATE TABLE `data_permissions`;
