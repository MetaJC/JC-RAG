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


package com.jc.rag.admin.module.rag.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Skill 保存参数。
 * @author JC
 */
@Data
public class RagSkillSaveParam {

    @NotBlank(message = "skillName 不能为空")
    @Size(max = 100)
    private String skillName;

    @NotBlank(message = "skillKey 不能为空")
    @Size(max = 100)
    private String skillKey;

    private String description;

    private String promptTemplate;

    private String triggerType;

    private String triggerRules;

    private String icon;

    private Integer sortOrder;

    private Integer status;
}
