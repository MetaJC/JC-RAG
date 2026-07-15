-- =============================================================================
-- 022_kb_permission.sql
-- -----------------------------------------------------------------------------
-- 目的: 知识库权限模型重构, 支持 PRIVATE/INTERNAL/PUBLIC 三档可见范围 + 三级操作权限
-- 业务规则:
--   1. visibility_scope 扩展为 PRIVATE / INTERNAL / PUBLIC (老值 ROLE 迁移到 INTERNAL)
--   2. 新增 client_selectable: 公开知识库是否允许客户端用户自行选择
--   3. 新增 kb_role_auth: 知识库对角色授权 (perm_level: 1 VIEW / 2 EDIT / 3 MANAGE)
--   4. 新增 kb_user_auth: 知识库对用户授权 (user_type=ADMIN 才允许授权)
-- 顺序: 在 021 之后执行
-- 兼容: 不可重复执行 (字段/表创建前均做存在性检测)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 知识库表新增 client_selectable 字段
-- -----------------------------------------------------------------------------
SET @kb_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'kb_knowledge_base') > 0,
    'kb_knowledge_base', NULL
);
SET @ddl := IF(
    @kb_table IS NOT NULL
    AND (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = @kb_table AND column_name = 'client_selectable') = 0,
    CONCAT('ALTER TABLE ', @kb_table,
           ' ADD COLUMN client_selectable TINYINT(1) NOT NULL DEFAULT 0',
           ' COMMENT ''公开知识库是否允许客户端用户自行选择 (0 否 / 1 是)''',
           ' AFTER visibility_scope'),
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. 历史数据迁移: visibility_scope=ROLE -> INTERNAL
-- -----------------------------------------------------------------------------
SET @ddl := IF(
    @kb_table IS NOT NULL,
    CONCAT('UPDATE ', @kb_table, " SET visibility_scope = 'INTERNAL' WHERE visibility_scope = 'ROLE'"),
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. 知识库-角色授权表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb_role_auth (
    id          BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '主键',
    kb_id       BIGINT       NOT NULL                COMMENT '知识库 ID',
    role_id     BIGINT       NOT NULL                COMMENT '角色 ID',
    perm_level  TINYINT      NOT NULL                COMMENT '权限级别: 1 查看 / 2 编辑 / 3 管理',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_role (kb_id, role_id),
    KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库-角色授权';

-- -----------------------------------------------------------------------------
-- 4. 知识库-用户授权表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb_user_auth (
    id          BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '主键',
    kb_id       BIGINT       NOT NULL                COMMENT '知识库 ID',
    user_id     BIGINT       NOT NULL                COMMENT '用户 ID (限定 user_type=ADMIN)',
    perm_level  TINYINT      NOT NULL                COMMENT '权限级别: 1 查看 / 2 编辑 / 3 管理',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_user (kb_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库-用户授权';

-- -----------------------------------------------------------------------------
-- 4.1 兼容已存在但 id 缺失 AUTO_INCREMENT 的旧表 (幂等)
-- -----------------------------------------------------------------------------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'kb_role_auth'
       AND column_name = 'id' AND extra NOT LIKE '%auto_increment%') > 0,
    'ALTER TABLE kb_role_auth MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'kb_user_auth'
       AND column_name = 'id' AND extra NOT LIKE '%auto_increment%') > 0,
    'ALTER TABLE kb_user_auth MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 5. 新增 "知识库授权" 菜单 (挂载在 知识库管理 子菜单, 复用 menu:knowledge_base 父级)
-- -----------------------------------------------------------------------------
SET @perm_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_permission') > 0,
    'rbac_permission', 'sys_permission'
);
SET @role_table := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_role') > 0,
    'rbac_role', 'sys_role'
);

SET @ddl := CONCAT('SELECT id INTO @kb_parent_id FROM ', @perm_table, " WHERE perm_code = 'menu:knowledge_base' LIMIT 1");
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @kb_parent_id = COALESCE(@kb_parent_id, 0);

-- 新增 / 更新 知识库授权 子菜单
SET @ddl := CONCAT(
    'INSERT INTO ', @perm_table,
    ' (perm_code, perm_name, perm_type, parent_id, path, icon, sort, component, visible, status) ',
    "VALUES ('menu:knowledge_auth', '知识库授权', 'MENU', ?, '/knowledge/auth', 'Key', 11, 'views/KnowledgeBaseAuthView.vue', 1, 1) ",
    'ON DUPLICATE KEY UPDATE ',
    'perm_name=VALUES(perm_name), perm_type=VALUES(perm_type), parent_id=VALUES(parent_id), ',
    'path=VALUES(path), icon=VALUES(icon), sort=VALUES(sort), component=VALUES(component), visible=1, status=1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt USING @kb_parent_id; DEALLOCATE PREPARE stmt;

-- 同步 super_admin / admin 角色的菜单绑定
SET @ddl := CONCAT(
    'INSERT IGNORE INTO ', @role_table, '_permission (role_id, permission_id) ',
    'SELECT r.id, p.id FROM ', @role_table, ' r JOIN ', @perm_table, ' p ON p.perm_code = ''menu:knowledge_auth'' ',
    "WHERE r.role_code IN ('super_admin', 'admin')"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 6. 验证
-- -----------------------------------------------------------------------------
SELECT column_name, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'kb_knowledge_base'
  AND column_name IN ('visibility_scope', 'client_selectable')
ORDER BY column_name;

SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('kb_role_auth', 'kb_user_auth')
ORDER BY table_name;
