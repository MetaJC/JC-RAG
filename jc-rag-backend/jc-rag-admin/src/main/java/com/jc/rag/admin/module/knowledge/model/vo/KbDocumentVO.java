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


package com.jc.rag.admin.module.knowledge.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档 VO。
 * <p>对应 kb_document 表, 用于列表 / 详情 / 上传回显 / 重建索引等场景。
 *
 * @author JC
 */
@Data
public class KbDocumentVO {
    private Long id;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String contentType;
    private Integer versionNo;
    private String docType;
    private String securityLevel;
    private String parseStatus;
    private String parseMessage;
    private Integer pageCount;
    private Integer chunkCount;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}
