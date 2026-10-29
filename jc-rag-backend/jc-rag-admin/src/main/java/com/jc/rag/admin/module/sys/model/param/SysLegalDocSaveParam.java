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


package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户协议 / 隐私政策 保存参数 (草稿 / 内容保存)。
 * @author JC
 */
public class SysLegalDocSaveParam {

    @NotBlank(message = "docType 不能为空")
    @Pattern(regexp = "USER_AGREEMENT|PRIVACY_POLICY", message = "docType 必须为 USER_AGREEMENT 或 PRIVACY_POLICY")
    private String docType;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过 255")
    private String title;

    /** Markdown 内容 (允许为空字符串, 提交时自动 trim) */
    private String contentMd;

    /** 状态: DRAFT(草稿) / PUBLISHED(发布); 空默认 DRAFT */
    @Pattern(regexp = "DRAFT|PUBLISHED", message = "status 必须为 DRAFT 或 PUBLISHED")
    private String status;

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
