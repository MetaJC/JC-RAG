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
 * 知识库 VO。
 * <p>对应 kb_knowledge_base 表, 用于列表 / 详情 / 创建 / 更新 / 默认知识库等场景。
 *
 * @author JC
 */
@Data
public class KnowledgeBaseVO {
    private Long id;
    private String kbCode;
    private String kbName;
    private String kbDesc;
    private String visibilityScope;
    /** 公开 KB 是否允许客户端用户自行选择 (0 否 / 1 是) */
    private Integer clientSelectable;
    /** 所有者用户 ID */
    private Long ownerUserId;
    /** 当前用户对该 KB 的最大权限级别 (1 VIEW / 2 EDIT / 3 MANAGE / 0 NONE), 仅列表场景填充 */
    private Integer myPermLevel;
    private Integer status;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
