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


package com.jc.rag.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 知识库 实体。
 * <p>对应 kb_knowledge_base 表, 隔离不同业务域的文档集合。
 * <p>核心字段: kbCode 业务编码, visibilityScope PRIVATE/INTERNAL/PUBLIC, status 启用状态, clientSelectable 是否允许客户端选择。
 *
 * @author JC
 */
@TableName("kb_knowledge_base")
public class KbKnowledgeBaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库业务编码 (全局唯一)
     */
    @TableField("kb_code")
    private String kbCode;

    /**
     * 知识库展示名
     */
    @TableField("kb_name")
    private String kbName;

    /**
     * 知识库描述
     */
    @TableField("kb_desc")
    private String kbDesc;

    /**
     * 可见性范围 (PRIVATE 私有 / INTERNAL 内部 / PUBLIC 公开)
     */
    @TableField("visibility_scope")
    private String visibilityScope;

    /**
     * 是否允许客户端用户从公开知识库中自行选择 (0 否 / 1 是, 仅 PUBLIC 生效)
     */
    @TableField("client_selectable")
    private Integer clientSelectable;

    /**
     * 所有者用户 ID (知识库创建人)
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 启用状态 (1 启用 / 0 禁用)
     */
    private Integer status;

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

    public void setId(Long id) {
        this.id = id;
    }

    public String getKbCode() {
        return kbCode;
    }

    public void setKbCode(String kbCode) {
        this.kbCode = kbCode;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getKbDesc() {
        return kbDesc;
    }

    public void setKbDesc(String kbDesc) {
        this.kbDesc = kbDesc;
    }

    public String getVisibilityScope() {
        return visibilityScope;
    }

    public void setVisibilityScope(String visibilityScope) {
        this.visibilityScope = visibilityScope;
    }

    public Integer getClientSelectable() {
        return clientSelectable;
    }

    public void setClientSelectable(Integer clientSelectable) {
        this.clientSelectable = clientSelectable;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
