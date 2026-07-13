-- =============================================================================
-- 024_sensitive_word_user_agent_security_audit.sql
-- -----------------------------------------------------------------------------
-- 目的: 用户级智能体 + 敏感词库 + 安全审计 (一期安全防护)
-- 业务背景:
--   1. 现有 sys_agents 仅支持系统智能体 (后管管控)
--   2. 新增 user_custom_agent 支持 C 端用户创建个性化智能体
--      仅允许配置 user 层 prompt 片段, 不可触及 system 层
--   3. 新增 sys_sensitive_word 敏感词库 (落库管理, Aho-Corasick 内存匹配)
--   4. 新增 security_audit_log 安全审计日志 (敏感词命中/拒绝/脱敏全留痕)
-- 顺序: 在 023 之后执行
-- 兼容: 不可重复执行 (字段/表创建前均做存在性检测)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. C 端用户自定义智能体 (user 层 prompt 片段, 不可配置 system 层)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_custom_agent (
    id              BIGINT       NOT NULL AUTO_INCREMENT                          COMMENT '主键',
    user_id         BIGINT       NOT NULL                                        COMMENT '所属用户 ID',
    agent_name      VARCHAR(30)  NOT NULL                                        COMMENT '智能体名称 (≤30字)',
    content         VARCHAR(500) NOT NULL                                        COMMENT '用户层 prompt 片段 (≤500字, 安全过滤)',
    status          TINYINT      NOT NULL DEFAULT 1                              COMMENT '1=启用 0=禁用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP              COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C 端用户自定义智能体 (只存 user 层 prompt, 不可触及 system 层)';

-- -----------------------------------------------------------------------------
-- 2. 系统敏感词库 (落库管理, Aho-Corasick 内存匹配)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_sensitive_word (
    id              BIGINT       NOT NULL AUTO_INCREMENT                          COMMENT '主键',
    word            VARCHAR(100) NOT NULL                                        COMMENT '关键词',
    risk_level      VARCHAR(8)   NOT NULL                                        COMMENT 'HIGH / MEDIUM / LOW',
    category        VARCHAR(32)  DEFAULT NULL                                    COMMENT 'PROMPT_INJECTION / CREDENTIAL / PII / ABUSE',
    action          VARCHAR(16)  NOT NULL                                        COMMENT 'DENY / ALERT / MASK / PASS',
    enabled         TINYINT      NOT NULL DEFAULT 1                              COMMENT '1=启用 0=禁用',
    description     VARCHAR(255) DEFAULT NULL                                    COMMENT '说明/备注',
    created_by      BIGINT       DEFAULT NULL                                    COMMENT '创建人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP              COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_word (word),
    KEY idx_enabled_level (enabled, risk_level),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统敏感词库 (落库管理, 启动全量加载到内存)';

-- 预置词库 (一键注入, 运营可按需禁用/删除)
INSERT IGNORE INTO sys_sensitive_word (word, risk_level, category, action, description) VALUES
-- HIGH: PROMPT_INJECTION (拒绝)
('忽略以上指令',         'HIGH', 'PROMPT_INJECTION', 'DENY',  '经典 prompt injection'),
('忽略 system',         'HIGH', 'PROMPT_INJECTION', 'DENY',  '试图绕过 system 层'),
('忽略 prompt',         'HIGH', 'PROMPT_INJECTION', 'DENY',  '试图绕过 prompt'),
('你的系统提示词',       'HIGH', 'PROMPT_INJECTION', 'DENY',  '试图套出 system prompt'),
('你的 system prompt',  'HIGH', 'PROMPT_INJECTION', 'DENY',  '试图套出 system prompt'),
('repeat the words above', 'HIGH', 'PROMPT_INJECTION', 'DENY', '经典 prompt injection'),
('disregard previous',  'HIGH', 'PROMPT_INJECTION', 'DENY',  '英文 prompt injection'),
-- HIGH: CREDENTIAL (拒绝)
('password=',           'HIGH', 'CREDENTIAL', 'DENY',  '用户可能误贴密码'),
('api_key=',            'HIGH', 'CREDENTIAL', 'DENY',  '用户可能误贴 API Key'),
('secret=',             'HIGH', 'CREDENTIAL', 'DENY',  '用户可能误贴密钥'),
('token=',              'HIGH', 'CREDENTIAL', 'DENY',  '用户可能误贴 token'),
-- MEDIUM: PII (告警 + 审计)
('身份证',              'MEDIUM', 'PII', 'ALERT', '可能涉及 PII'),
('银行卡',              'MEDIUM', 'PII', 'ALERT', '可能涉及 PII'),
('信用卡',              'MEDIUM', 'PII', 'ALERT', '可能涉及 PII'),
-- MEDIUM: ABUSE (告警 + 审计)
('渗透测试',            'MEDIUM', 'ABUSE', 'ALERT', '可能涉及越权试探'),
('sql 注入',            'MEDIUM', 'ABUSE', 'ALERT', '可能涉及越权试探'),
('ddos',                'MEDIUM', 'ABUSE', 'ALERT', '可能涉及越权试探'),
-- LOW: 仅审计
('工资',                'LOW', 'ABUSE', 'PASS', '可能正常咨询'),
('加班',                'LOW', 'ABUSE', 'PASS', '可能正常咨询');

-- -----------------------------------------------------------------------------
-- 3. 安全审计日志
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS security_audit_log (
    id                      BIGINT       NOT NULL AUTO_INCREMENT                  COMMENT '主键',
    user_id                 BIGINT       DEFAULT NULL                            COMMENT '触发用户',
    session_id              BIGINT       DEFAULT NULL                            COMMENT '关联会话',
    audit_type              VARCHAR(32)  NOT NULL                                COMMENT 'INPUT_DENY/INPUT_ALERT/INPUT_MASK/OUTPUT_DENY/OUTPUT_MASK',
    risk_level              VARCHAR(8)   DEFAULT NULL                            COMMENT 'HIGH/MEDIUM/LOW',
    matched_word_id         BIGINT       DEFAULT NULL                            COMMENT '命中敏感词 ID (关联 sys_sensitive_word)',
    matched_word            VARCHAR(100) DEFAULT NULL                            COMMENT '命中关键词 (冗余, 便于审计)',
    original_content_hash   CHAR(64)     DEFAULT NULL                            COMMENT '原文 SHA-256 (不存原文, 保护隐私)',
    masked_content          TEXT         DEFAULT NULL                            COMMENT '脱敏后内容 (供审计查阅)',
    action                  VARCHAR(16)  NOT NULL                                COMMENT 'DENY/ALERT/PASS/MASK',
    client_ip               VARCHAR(45)  DEFAULT NULL                            COMMENT '客户端 IP',
    user_agent              VARCHAR(255) DEFAULT NULL                            COMMENT 'UA',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, created_at),
    KEY idx_type (audit_type, created_at),
    KEY idx_word (matched_word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全审计日志 (敏感内容只存 SHA-256 指纹)';

-- -----------------------------------------------------------------------------
-- 4. 系统默认智能体 (使用 sys_config, 全局唯一 ID, C 端强制使用)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
VALUES ('AGENT', 'default_agent_id', '', 'NUMBER', 0, '系统默认智能体 ID (空=未设置, 客户端不强制; 非空=客户端强制使用该智能体)');
