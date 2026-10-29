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


package com.jc.rag.admin.module.knowledge.model.param;

import jakarta.validation.constraints.NotBlank;

public class KnowledgeBaseUpdateParam {

    @NotBlank(message = "知识库名称不能为空")
    private String kbName;

    private String kbDesc;

    private String visibilityScope;

    /** 是否允许客户端用户自行选择 (0 否 / 1 是) - 仅 PUBLIC 生效 */
    private Integer clientSelectable;

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
}

