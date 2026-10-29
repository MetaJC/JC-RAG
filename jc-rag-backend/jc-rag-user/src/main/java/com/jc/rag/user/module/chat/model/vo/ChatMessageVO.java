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


package com.jc.rag.user.module.chat.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * C 端消息 VO。
 * <p>对应 chat_message 表, attachments 为反序列化后的多模态附件列表 (缩略图 / OCR / 文件元信息)。
 *
 * @author JC
 */
public class ChatMessageVO {
    private Long id;
    private String roleType;
    private String messageContent;
    private Integer messageIndex;
    private Integer tokenCount;
    private Long latencyMs;
    private LocalDateTime createdAt;
    private List<Map<String, Object>> attachments;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public Integer getMessageIndex() { return messageIndex; }
    public void setMessageIndex(Integer messageIndex) { this.messageIndex = messageIndex; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Map<String, Object>> getAttachments() { return attachments; }
    public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }

    @Override
    public String toString() {
        return "ChatMessageVO{id=" + id + ", roleType='" + roleType + "', messageIndex=" + messageIndex + ", tokenCount=" + tokenCount + ", latencyMs=" + latencyMs + '}';
    }
}
