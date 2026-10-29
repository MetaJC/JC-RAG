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


package com.jc.rag.domain.entity.legal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 法律文档 实体 (admin/user 共用)。
 * <p>对应 sys_legal_doc 表, 存用户协议/隐私政策的 Markdown 内容及发布状态。
 * <p>核心字段: docType USER_AGREEMENT / PRIVACY_POLICY, status DRAFT / PUBLISHED, publishedAt/publishedBy 发布信息。
 *
 * @author JC
 */
@TableName("sys_legal_doc")
public class SysLegalDocEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 文档类型 (USER_AGREEMENT 用户协议 / PRIVACY_POLICY 隐私政策)
     */
    @TableField("doc_type")
    private String docType;
    /**
     * 文档标题
     */
    private String title;
    /**
     * 文档 Markdown 内容
     */
    @TableField("content_md")
    private String contentMd;
    /**
     * 源文件名 (导入时记录)
     */
    @TableField("source_filename")
    private String sourceFilename;
    /**
     * 状态 (DRAFT 草稿 / PUBLISHED 已发布 / ARCHIVED 归档)
     */
    private String status;
    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;
    /**
     * 发布人用户 ID
     */
    @TableField("published_by")
    private Long publishedBy;
    /**
     * 最近修改人用户 ID
     */
    @TableField("updated_by")
    private Long updatedBy;
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

    public String getDocType() {
        return docType;
    }

    public void setDocType(String v) {
        docType = v;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getContentMd() {
        return contentMd;
    }

    public void setContentMd(String v) {
        contentMd = v;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(String v) {
        sourceFilename = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime v) {
        publishedAt = v;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(Long v) {
        publishedBy = v;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long v) {
        updatedBy = v;
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
