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


package com.jc.rag.domain.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * C 端用户自定义智能体实体。
 * <p>对应 user_custom_agent 表, 仅存 user 层 prompt 片段, 不可触及 system 层。
 * <p>配额: 单用户最多 MAX_PER_USER 条 (默认 3, Pro 版 10, 通过 jc-rag.user-agent.max-per-user 配置)。
 * <p>安全: 写入时先经 PII 脱敏 + 敏感词过滤, 命中 DENY 直接拒绝, 命中 ALERT/MASK 落审计。
 *
 * @author JC
 */
@TableName("user_custom_agent")
public class UserCustomAgentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属用户 ID */
    @TableField("user_id")
    private Long userId;
    /** 智能体名称 (≤30字) */
    @TableField("agent_name")
    private String agentName;
    /** 用户层 prompt 片段 (≤500字, 安全过滤) */
    private String content;
    /** 1=启用 0=禁用 */
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String v) { agentName = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
