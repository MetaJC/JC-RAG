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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库上传异常响应 VO。
 * <p>前端可依据 code 判定业务失败，并按 maxFileSizeBytes/maxRequestSizeBytes 给出友好提示。
 *
 * @author JC
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeUploadErrorVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 错误大类，如 "上传文件超出限制" */
    private String error;

    /** 详细错误信息 */
    private String message;

    /** 单文件上限字节数 */
    private Long maxFileSizeBytes;

    /** 单次上传总量上限字节数 */
    private Long maxRequestSizeBytes;

    /** 单文件上限展示文本 */
    private String maxFileSizeLabel;

    /** 单次上传总量上限展示文本 */
    private String maxRequestSizeLabel;
}
