-- =============================================================================
-- 019_legal_doc_and_login_fail_count.sql
-- -----------------------------------------------------------------------------
-- 目的: 用户协议 / 隐私政策 + 登录失败计数 新增
-- 顺序: 在 018 之后执行
-- 兼容: sys_user 已重命名为 rbac_user 的库自动跳过 user 表 DDL
-- =============================================================================

-- 1. sys_legal_doc (用户协议 / 隐私政策)
CREATE TABLE IF NOT EXISTS sys_legal_doc (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    doc_type        VARCHAR(32)     NOT NULL                COMMENT '文档类型: USER_AGREEMENT / PRIVACY_POLICY',
    title           VARCHAR(255)    NOT NULL                COMMENT '标题',
    content_md      MEDIUMTEXT      NOT NULL                COMMENT 'Markdown 内容',
    source_filename VARCHAR(255)    NULL                    COMMENT '原始上传文件名',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT / PUBLISHED',
    published_at    DATETIME        NULL                    COMMENT '发布时间',
    published_by    BIGINT          NULL                    COMMENT '发布人',
    updated_by      BIGINT          NULL                    COMMENT '最后修改人',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_type (doc_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议/隐私政策';

-- 2. login_fail_count 列 (兼容 sys_user / rbac_user)
-- 注意: MySQL 8.0.29+ 才支持 IF NOT EXISTS, 低版本请先确认列不存在再 ADD
-- 优先尝试 sys_user, 失败回退 rbac_user
SET @ddl_user := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_user') > 0,
    'ALTER TABLE sys_user ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0 COMMENT ''连续登录失败次数''',
    IF(
        (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rbac_user') > 0,
        'ALTER TABLE rbac_user ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0 COMMENT ''连续登录失败次数''',
        'SELECT ''no user table found'' AS warn'
    )
);
PREPARE stmt FROM @ddl_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 验证
SELECT
    (SELECT COUNT(*) FROM sys_legal_doc)       AS legal_doc_count,
    (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND ((table_name = 'sys_user' OR table_name = 'rbac_user'))
         AND column_name = 'login_fail_count') AS login_fail_count_col;

-- =============================================================================
-- 初始化: 可选 - 插入默认占位协议草稿
-- =============================================================================
INSERT IGNORE INTO sys_legal_doc (doc_type, title, content_md, status) VALUES
('USER_AGREEMENT', '用户协议', '# 用户协议\n\n请补充具体条款内容。', 'DRAFT'),
('PRIVACY_POLICY', '隐私政策', '# 隐私政策\n\n请补充具体条款内容。', 'DRAFT');
