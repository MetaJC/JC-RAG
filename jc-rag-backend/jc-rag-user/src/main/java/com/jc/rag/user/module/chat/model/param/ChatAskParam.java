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


package com.jc.rag.user.module.chat.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * C 端问答请求 Param (普通问答 + 流式问答共用)。
 * @author JC
 */
@Data
public class ChatAskParam {

    @NotBlank(message = "question 不能为空")
    private String question;

    /** 是否开启深度思考. */
    private Boolean deepThinking;

    /** 指定智能体 ID (可选). */
    private Long agentId;

    /**
     * 自定义智能体 ID (可选, 互斥于 agentId).
     * <p>传入时, Java 端会查 user_custom_agent 表, 取其 content 字段作为 user 层 prompt 片段注入,
     * 不会触及 system 层. 与系统 agentId 互斥: 优先使用 customAgentId.
     */
    private Long customAgentId;

    /** 覆盖会话的 KB (可选). */
    private Long kbId;

    /** 附件列表 (可选). */
    private List<Map<String, Object>> attachments;
}
