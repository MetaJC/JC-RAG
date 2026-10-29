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


package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 会话滚动摘要实体 (L1 摘要压缩)。
 * <p>对应 chat_session_summary 表, 记录本会话已被 LLM 压缩过的历史区间及摘要正文。
 * <p>核心字段: coveredFromIndex / coveredToIndex 标识该摘要覆盖的 message_index 区间 (含两端),
 * status 标识是否仍有效 (同会话再次压缩时旧摘要会被置为 0 失效)。
 *
 * @author JC
 */
@TableName("chat_session_summary")
public class ChatSessionSummaryEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 所属会话 ID
     */
    @TableField("session_id")
    private Long sessionId;
    /**
     * 所属用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 摘要正文 (脱敏后)
     */
    @TableField("summary_text")
    private String summaryText;
    /**
     * 覆盖 message_index 起始 (含)
     */
    @TableField("covered_from_index")
    private Integer coveredFromIndex;
    /**
     * 覆盖 message_index 结束 (含)
     */
    @TableField("covered_to_index")
    private Integer coveredToIndex;
    /**
     * 摘要 token 估算 (按 1 token ≈ 1.5 字符估算, 仅供前端展示)
     */
    @TableField("token_count")
    private Integer tokenCount;
    /**
     * 状态: 1=有效, 0=失效 (同会话再次压缩后旧摘要会被置为 0)
     */
    @TableField("status")
    private Integer status;
    /**
     * 生成该摘要使用的模型名 (用于排查/审计)
     */
    @TableField("model_name")
    private String modelName;
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long v) {
        sessionId = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String v) {
        summaryText = v;
    }

    public Integer getCoveredFromIndex() {
        return coveredFromIndex;
    }

    public void setCoveredFromIndex(Integer v) {
        coveredFromIndex = v;
    }

    public Integer getCoveredToIndex() {
        return coveredToIndex;
    }

    public void setCoveredToIndex(Integer v) {
        coveredToIndex = v;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer v) {
        tokenCount = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String v) {
        modelName = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }
}
