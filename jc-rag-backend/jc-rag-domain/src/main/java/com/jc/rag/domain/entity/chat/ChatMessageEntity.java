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
 * 聊天消息实体。
 * <p>对应 chat_message 表, 记录会话内每一条消息 (user / assistant / system)。
 * <p>核心字段: sessionId 所属会话, roleType 角色类型, messageIndex 会话内序号, tokenCount 消耗 Token 数。
 *
 * @author JC
 */
@TableName("chat_message")
public class ChatMessageEntity {
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
     * 角色类型 (user / assistant / system / tool)
     */
    @TableField("role_type")
    private String roleType;
    /**
     * 消息正文 (Markdown / 纯文本)
     */
    @TableField("message_content")
    private String messageContent;
    /**
     * 会话内序号 (用于还原对话顺序)
     */
    @TableField("message_index")
    private Integer messageIndex;
    /**
     * 本条消息消耗 Token 数
     */
    @TableField("token_count")
    private Integer tokenCount;
    /**
     * 大模型调用耗时 (毫秒)
     */
    @TableField("latency_ms")
    private Long latencyMs;
    /**
     * 附件 JSON (图片 / 文件列表)
     */
    @TableField("attachments")
    private String attachments;
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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long v) {
        sessionId = v;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String v) {
        roleType = v;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String v) {
        messageContent = v;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer v) {
        messageIndex = v;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer v) {
        tokenCount = v;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long v) {
        latencyMs = v;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String v) {
        attachments = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
