package com.jc.rag.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ServerModuleSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ServerModuleSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public ServerModuleSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureTables() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS demo_config (
                    id                    BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
                    demo_mode_enabled     TINYINT     NOT NULL DEFAULT 0       COMMENT '演示模式开关: 0=关闭, 1=开启',
                    default_quota_per_day INT         NOT NULL DEFAULT 20      COMMENT '默认每日问答次数',
                    default_tokens_per_q  INT         NOT NULL DEFAULT 2048    COMMENT '每次问答最大Token数',
                    default_tokens_per_day INT        NOT NULL DEFAULT 10000   COMMENT '每日最大Token消耗',
                    upgrade_hint          VARCHAR(500) NULL                    COMMENT '超出限制时的提示语',
                    updated_by            BIGINT      NULL                     COMMENT '最后修改人',
                    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示模式全局配置'
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS role_quota_limits (
                    id                    BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
                    role_id               BIGINT      NOT NULL                 COMMENT '角色ID',
                    quota_per_day         INT         NULL                     COMMENT '每日问答次数',
                    tokens_per_q          INT         NULL                     COMMENT '每次问答最大Token数',
                    tokens_per_day        INT         NULL                     COMMENT '每日最大Token消耗',
                    can_configure_api_key TINYINT     NOT NULL DEFAULT 1       COMMENT '是否允许配置自己的API Key',
                    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_role_id (role_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色配额限制配置'
                """);

            log.info("管理端基础表结构检查完成: demo_config, role_quota_limits");

            // 用户协议 / 隐私政策 (Markdown 内容, doc_type 唯一)
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_legal_doc (
                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                    doc_type VARCHAR(32) NOT NULL COMMENT '文档类型: USER_AGREEMENT / PRIVACY_POLICY',
                    title VARCHAR(255) NOT NULL COMMENT '标题',
                    content_md MEDIUMTEXT NOT NULL COMMENT 'Markdown 内容',
                    source_filename VARCHAR(255) NULL COMMENT '原始上传文件名',
                    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT / PUBLISHED',
                    published_at DATETIME NULL COMMENT '发布时间',
                    published_by BIGINT NULL COMMENT '发布人',
                    updated_by BIGINT NULL COMMENT '最后修改人',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_doc_type (doc_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议/隐私政策 (sys 模块)'
                """);

            log.info("管理端基础表结构检查完成: sys_legal_doc");
        } catch (Exception e) {
            log.warn("管理端基础表结构初始化失败: {}", e.getMessage());
        }
    }
}
