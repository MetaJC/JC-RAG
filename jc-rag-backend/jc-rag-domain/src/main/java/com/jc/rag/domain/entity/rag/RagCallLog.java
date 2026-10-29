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


package com.jc.rag.domain.entity.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * RAG 调用日志。
 * <p>对应 rag_call_log 表, 由 ChatService 在每次 RAG 调用时写入, 用于成本核算与质量回溯。
 *
 * @author JC
 */
@TableName("rag_call_log")
public class RagCallLog {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 发起调用的用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 所属会话 ID
     */
    @TableField("session_id")
    private Long sessionId;
    /**
     * 用户原始问题
     */
    @TableField("query_text")
    private String queryText;
    /**
     * Query Rewrite 之后的问题 (含历史上下文)
     */
    @TableField("rewritten_query")
    private String rewrittenQuery;
    /**
     * 命中向量库的切片数
     */
    @TableField("retrieved_chunks")
    private Integer retrievedChunks;
    /**
     * 模型类型
     */
    @TableField("model_mode")
    private String modelMode;
    /**
     * 具体模型名
     */
    @TableField("model_name")
    private String modelName;
    /**
     * Prompt Token 数
     */
    @TableField("prompt_tokens")
    private Integer promptTokens;
    /**
     * Completion Token 数
     */
    @TableField("completion_tokens")
    private Integer completionTokens;
    /**
     * 调用耗时 (毫秒)
     */
    @TableField("latency_ms")
    private Long latencyMs;
    /**
     * 调用是否成功 (1 成功 / 0 失败)
     */
    @TableField("success_flag")
    private Integer successFlag;
    /**
     * 失败时的错误信息
     */
    @TableField("error_message")
    private String errorMessage;
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long v) {
        sessionId = v;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String v) {
        queryText = v;
    }

    public String getRewrittenQuery() {
        return rewrittenQuery;
    }

    public void setRewrittenQuery(String v) {
        rewrittenQuery = v;
    }

    public Integer getRetrievedChunks() {
        return retrievedChunks;
    }

    public void setRetrievedChunks(Integer v) {
        retrievedChunks = v;
    }

    public String getModelMode() {
        return modelMode;
    }

    public void setModelMode(String v) {
        modelMode = v;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String v) {
        modelName = v;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer v) {
        promptTokens = v;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer v) {
        completionTokens = v;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long v) {
        latencyMs = v;
    }

    public Integer getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(Integer v) {
        successFlag = v;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String v) {
        errorMessage = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
