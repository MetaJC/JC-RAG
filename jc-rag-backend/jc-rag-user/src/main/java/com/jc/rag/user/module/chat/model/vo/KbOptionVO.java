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

/**
 * C 端"可选知识库"VO。
 * <p>用于 /api/chat/knowledge-bases 列出 C 端允许选择的知识库 (含默认标记)。
 * <p>isDefault: 1=当前会话的默认知识库, 0=其他可选。
 *
 * @author JC
 */
public class KbOptionVO {
    private Long id;
    private String kbCode;
    private String kbName;
    private String kbDesc;
    private Integer isDefault;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKbCode() { return kbCode; }
    public void setKbCode(String kbCode) { this.kbCode = kbCode; }
    public String getKbName() { return kbName; }
    public void setKbName(String kbName) { this.kbName = kbName; }
    public String getKbDesc() { return kbDesc; }
    public void setKbDesc(String kbDesc) { this.kbDesc = kbDesc; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    @Override
    public String toString() {
        return "KbOptionVO{id=" + id + ", kbCode='" + kbCode + "', kbName='" + kbName + "', isDefault=" + isDefault + '}';
    }
}
