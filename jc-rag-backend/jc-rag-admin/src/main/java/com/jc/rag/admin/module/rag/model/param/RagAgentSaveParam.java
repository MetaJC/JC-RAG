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


package com.jc.rag.admin.module.rag.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Agent 保存参数。
 * @author JC
 */
@Data
public class RagAgentSaveParam {

    @NotBlank(message = "agentName 不能为空")
    @Size(max = 100)
    private String agentName;

    /**
     * 业务 Key (全局唯一, 由后端按 agentName 自动 slug 生成; 留空表示自动生成)
     */
    @Size(max = 100)
    private String agentKey;

    private String description;

    /**
     * 智能体类型: 1=系统智能体 (后台管控, C 端不可见, 可作为默认强制使用) | 0=内建用户智能体 (后台创建, C 端所有用户可见)
     * <p>必填; 系统智能体 (1) 必填 systemPrompt, 内建用户智能体 (0) 必填 content。
     */
    private Integer isSystem;

    /**
     * System Prompt (系统智能体 isSystem=1 使用, 必填)
     */
    private String systemPrompt;

    /**
     * User Prompt (内建用户智能体 isSystem=0 使用, 必填)
     * <p>对应 user_custom_agent.content 语义, 作为 LLM 的 user message 增强片段。
     */
    private String content;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Integer status;

    private java.util.List<Long> skillIds;
}
