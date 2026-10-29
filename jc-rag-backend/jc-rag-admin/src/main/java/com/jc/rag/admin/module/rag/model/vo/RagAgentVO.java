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


package com.jc.rag.admin.module.rag.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 视图。
 * @author JC
 */
@Data
public class RagAgentVO {
    private Long id;
    private String agentName;
    private String agentKey;
    /** 1=系统智能体 (Admin 管控, C 端不可见) 0=内建用户智能体 (Admin 创建, C 端可见) */
    private Integer isSystem;
    private String description;
    /** System Prompt (系统智能体 isSystem=1 使用) */
    private String systemPrompt;
    /** User Prompt (内建用户智能体 isSystem=0 使用, 对应 user_custom_agent.content 语义) */
    private String content;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer status;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
    private Long createdBy;
    private java.util.List<Long> skillIds;
}
