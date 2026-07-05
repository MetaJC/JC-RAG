-- ============================================================
-- JC RAG - 新功能模块数据库脚本
-- 遵守开闭原则：不修改已有表结构，仅新增表和扩展字段
-- ============================================================

-- 1. 系统提示词管理
CREATE TABLE IF NOT EXISTS `sys_prompts` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `prompt_key`      VARCHAR(100)    NOT NULL                 COMMENT '提示词标识键（如：chat_qa, chat_general, agent_xxx）',
    `prompt_name`     VARCHAR(200)    NOT NULL                 COMMENT '提示词名称（显示用）',
    `prompt_content`  TEXT            NOT NULL                 COMMENT '提示词内容',
    `scope`           VARCHAR(50)     NOT NULL DEFAULT 'global' COMMENT '作用域: global=全局, role=按角色, agent=按Agent',
    `scope_id`        BIGINT          NULL                     COMMENT '作用域关联ID（role_id / agent_id）',
    `version`         INT             NOT NULL DEFAULT 1       COMMENT '版本号',
    `status`          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 0=禁用, 1=启用',
    `description`     VARCHAR(500)    NULL                     COMMENT '备注说明',
    `created_by`      BIGINT          NULL                     COMMENT '创建人',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prompt_key` (`prompt_key`, `scope`, `scope_id`),
    KEY `idx_scope` (`scope`, `scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统提示词管理';

-- 2. Skills（技能）定义
CREATE TABLE IF NOT EXISTS `sys_skills` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `skill_name`      VARCHAR(200)    NOT NULL                 COMMENT '技能名称',
    `skill_key`       VARCHAR(100)    NOT NULL                 COMMENT '技能唯一标识',
    `description`     VARCHAR(1000)   NULL                     COMMENT '技能描述',
    `prompt_template` TEXT            NULL                     COMMENT '技能触发的提示词模板',
    `trigger_type`    VARCHAR(50)     NOT NULL DEFAULT 'auto'  COMMENT '触发方式: auto=自动, keyword=关键词, manual=手动',
    `trigger_rules`   VARCHAR(500)    NULL                     COMMENT '触发规则（JSON格式，如关键词列表）',
    `icon`            VARCHAR(100)    NULL                     COMMENT '图标',
    `sort_order`      INT             NOT NULL DEFAULT 0       COMMENT '排序号',
    `status`          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 0=禁用, 1=启用',
    `created_by`      BIGINT          NULL                     COMMENT '创建人',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_key` (`skill_key`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能定义表';

-- 3. Agent 定义
CREATE TABLE IF NOT EXISTS `sys_agents` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `agent_name`      VARCHAR(200)    NOT NULL                 COMMENT 'Agent 名称',
    `agent_key`       VARCHAR(100)    NOT NULL                 COMMENT 'Agent 唯一标识',
    `description`     VARCHAR(1000)   NULL                     COMMENT 'Agent 描述',
    `model_provider`  VARCHAR(50)     NULL                     COMMENT '模型提供商（ollama/openai/azure等）',
    `model_name`      VARCHAR(200)    NULL                     COMMENT '模型名称',
    `system_prompt`   TEXT            NULL                     COMMENT 'Agent 专用的系统提示词',
    `temperature`     DECIMAL(3,2)    NULL DEFAULT 0.7         COMMENT '温度参数',
    `max_tokens`      INT             NULL DEFAULT 2048        COMMENT '最大Token数',
    `status`          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态: 0=草稿, 1=已发布, 2=已下架',
    `version`         INT             NOT NULL DEFAULT 1       COMMENT '版本号',
    `published_at`    DATETIME        NULL                     COMMENT '发布时间',
    `created_by`      BIGINT          NULL                     COMMENT '创建人',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_key` (`agent_key`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 定义表';

-- 4. Agent-Skill 关联
CREATE TABLE IF NOT EXISTS `sys_agent_skills` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `agent_id`        BIGINT          NOT NULL                 COMMENT 'Agent ID',
    `skill_id`        BIGINT          NOT NULL                 COMMENT 'Skill ID',
    `sort_order`      INT             NOT NULL DEFAULT 0       COMMENT '排序号',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_skill` (`agent_id`, `skill_id`),
    KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent-Skill 关联表';

-- 5. Agent-角色关联（哪些角色可以使用哪些 Agent）
CREATE TABLE IF NOT EXISTS `sys_agent_roles` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `agent_id`        BIGINT          NOT NULL                 COMMENT 'Agent ID',
    `role_id`         BIGINT          NOT NULL                 COMMENT '角色ID',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_role` (`agent_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent-角色关联表';

-- 6. 演示模式全局配置
CREATE TABLE IF NOT EXISTS `demo_config` (
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `demo_mode_enabled`     TINYINT     NOT NULL DEFAULT 0       COMMENT '演示模式开关: 0=关闭, 1=开启',
    `default_quota_per_day` INT         NOT NULL DEFAULT 20      COMMENT '默认每日问答次数',
    `default_tokens_per_q`  INT         NOT NULL DEFAULT 2048    COMMENT '每次问答最大Token数',
    `default_tokens_per_day` INT        NOT NULL DEFAULT 10000   COMMENT '每日最大Token消耗',
    `upgrade_hint`          VARCHAR(500) NULL                    COMMENT '超出限制时的提示语',
    `updated_by`            BIGINT      NULL                     COMMENT '最后修改人',
    `created_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示模式全局配置';

-- 7. 角色配额限制（覆盖默认值）
CREATE TABLE IF NOT EXISTS `role_quota_limits` (
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `role_id`               BIGINT      NOT NULL                 COMMENT '角色ID',
    `quota_per_day`         INT         NULL                     COMMENT '每日问答次数（NULL=使用默认值）',
    `tokens_per_q`          INT         NULL                     COMMENT '每次问答最大Token数（NULL=使用默认值）',
    `tokens_per_day`        INT         NULL                     COMMENT '每日最大Token消耗（NULL=使用默认值）',
    `can_configure_api_key` TINYINT     NOT NULL DEFAULT 1       COMMENT '是否允许配置自己的API Key',
    `created_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色配额限制配置';

-- 8. 用户自定义 API Key
CREATE TABLE IF NOT EXISTS `user_api_keys` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `user_id`         BIGINT          NOT NULL                 COMMENT '用户ID',
    `provider`        VARCHAR(50)     NOT NULL                 COMMENT '提供商: openai/ollama/azure/自定义',
    `base_url`        VARCHAR(500)    NULL                     COMMENT 'API 地址',
    `api_key`         VARCHAR(500)    NOT NULL                 COMMENT '加密后的 API Key',
    `model_name`      VARCHAR(200)    NULL                     COMMENT '默认模型',
    `is_active`       TINYINT         NOT NULL DEFAULT 1       COMMENT '是否启用',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义 API Key';

-- 9. 用户用量记录
CREATE TABLE IF NOT EXISTS `user_quota_usage` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `user_id`         BIGINT          NOT NULL                 COMMENT '用户ID',
    `usage_date`      DATE            NOT NULL                 COMMENT '使用日期',
    `query_count`     INT             NOT NULL DEFAULT 0       COMMENT '当日问答次数',
    `tokens_used`     INT             NOT NULL DEFAULT 0       COMMENT '当日Token消耗',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `usage_date`),
    KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户用量日记录';

-- 10. 数据级权限表（知识库/文档级权限控制）
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

-- ============================================================
-- 初始数据：默认系统提示词
-- ============================================================
INSERT INTO `sys_prompts` (`prompt_key`, `prompt_name`, `prompt_content`, `scope`, `status`, `description`) VALUES
('chat_qa', '问答提示词（默认）', '你是军事领域RAG问答助手。优先基于给定知识片段作答。如果知识片段不足以回答，可以结合常识自由作答，不要输出固定话术。', 'global', 1, '默认的 RAG 问答系统提示词'),
('chat_general', '通用对话提示词', '直接用简洁的语言回答用户问题。', 'global', 1, '无知识库命中时的通用提示词'),
('agent_default', 'Agent 默认提示词', '你是一个智能助手。请根据你的知识和上下文回答用户的问题。', 'global', 1, 'Agent 的默认系统提示词');

-- ============================================================
-- 初始数据：默认演示配置
-- ============================================================
INSERT INTO `demo_config` (`demo_mode_enabled`, `default_quota_per_day`, `default_tokens_per_q`, `default_tokens_per_day`, `upgrade_hint`) VALUES
(0, 20, 2048, 10000, '您当日的问答次数已用完。如需继续使用，请在个人中心配置您自己的 API Key。');
