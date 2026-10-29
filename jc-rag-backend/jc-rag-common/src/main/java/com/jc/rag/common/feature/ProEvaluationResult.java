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


package com.jc.rag.common.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话评估结果 (社区版 / 付费版共用 DTO).
 * <p>社区版返回占位结果 (未启用), 付费版返回 LLM 真实评估.</p>
 *
 * @author JC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProEvaluationResult {

    /** 0-100 综合评分. */
    private Integer score;

    /** 评语 (LLM 生成). */
    private String comment;

    /** 优点 (LLM 提取). */
    private List<String> strengths;

    /** 改进建议 (LLM 提取). */
    private List<String> improvements;

    /** 评估所用模型名称. */
    private String modelName;

    /** 是否为付费增强版 (true = 付费版, false = 社区版占位). */
    private boolean proEnhanced;
}
