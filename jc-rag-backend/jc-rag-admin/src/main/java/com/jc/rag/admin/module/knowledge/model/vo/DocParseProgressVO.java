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
import java.util.List;
import java.util.Map;

/**
 * 文档解析进度 VO。
 * <p>stages / logs 字段保留 Map 形态, 来源于 Python 端原始 JSON 透传, 不做 DTO 化。
 *
 * @author JC
 */
@Data
public class DocParseProgressVO {
    private Long documentId;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String parseStatus;
    private Integer overallProgress;
    private String currentStage;
    private List<Map<String, Object>> stages;
    private List<Map<String, Object>> logs;
    private LocalDateTime updatedAt;
}
