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

import java.io.Serializable;
import java.util.List;

/**
 * 高级问答分析结果 (商业付费功能).
 *
 * @author JC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProAnalysisResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总会话数 */
    private Long totalSessions;

    /** 平均会话轮次 */
    private Double avgTurns;

    /** 高频提问关键词 Top10 */
    private List<String> topKeywords;

    /** 用户满意度评分 (0-100, 商业版基于多轮反馈计算; 开源版恒为 0) */
    private Integer satisfactionScore;

    /** 是否为商业增强结果 */
    private boolean proEnhanced;
}
