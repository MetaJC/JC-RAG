package com.jc.rag.user.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐 C 端运行所需的最小表结构，避免历史库未执行增量脚本时直接导致提问失败。
 */
@Component
public class UserModuleSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(UserModuleSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public UserModuleSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_api_keys (
                id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                provider VARCHAR(50) NOT NULL COMMENT '提供商: openai/ollama/azure/自定义',
                base_url VARCHAR(500) NULL COMMENT 'API 地址',
                api_key VARCHAR(500) NOT NULL COMMENT 'API Key',
                model_name VARCHAR(200) NULL COMMENT '默认模型',
                is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_user_id (user_id),
                KEY idx_provider (provider)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义 API Key'
            """);

        // 登录失败计数 (账号密码登录连续失败达到阈值后强制滑动验证)
        // 表名兼容: 历史库可能已将 sys_user 重命名为 rbac_user
        try {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数'");
        } catch (Exception ignore) {
            try {
                jdbcTemplate.execute("ALTER TABLE rbac_user ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数'");
            } catch (Exception ignore2) {
                log.warn("sys_user/rbac_user 添加 login_fail_count 失败, 跳过");
            }
        }

        // 用户协议 / 隐私政策 (管理端写入, 用户端只读)
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议/隐私政策'
            """);

        // Pro 付费功能: 对话质量评估记录表 (社区版也会建, 只是不会写)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_dialogue_evaluation (
                id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                session_id BIGINT NOT NULL COMMENT '会话ID',
                score INT NOT NULL DEFAULT 0 COMMENT '0-100 综合评分',
                comment VARCHAR(2000) NULL COMMENT '评语',
                strengths VARCHAR(2000) NULL COMMENT '优点 JSON 数组',
                improvements VARCHAR(2000) NULL COMMENT '改进建议 JSON 数组',
                model_name VARCHAR(100) NULL COMMENT '评估所用模型',
                pro_enhanced TINYINT NOT NULL DEFAULT 0 COMMENT '是否付费版评估 0/1',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_user_id (user_id),
                KEY idx_session_id (session_id),
                KEY idx_created_at (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话质量评估记录 (Pro 付费功能)'
            """);

        log.info("C 端基础表结构检查完成: user_api_keys, sys_user.login_fail_count, sys_legal_doc, user_dialogue_evaluation");
    }
}
