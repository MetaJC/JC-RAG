/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
